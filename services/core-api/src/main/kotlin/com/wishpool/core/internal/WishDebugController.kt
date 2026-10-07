package com.wishpool.core.internal

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.shared.ForbiddenError
import com.wishpool.core.shared.NotFoundError
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.web.bind.annotation.*
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.util.UUID

data class DebugFragmentsRequest(val earnedFragments: Int)
data class DebugWishResponse(val wishId: UUID, val familyId: UUID, val childId: UUID, val earnedFragments: Int, val requiredFragments: Int, val status: String, val fragmentMask: String?, val auditId: UUID? = null)

@RestController
class WishDebugController(
    private val jdbc: JdbcClient,
    private val auth: InternalAuthService,
    private val events: DomainEventPublisher,
    private val mapper: ObjectMapper,
    @Value("\${wishpool.debug-endpoints.enabled:false}") private val enabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @GetMapping("/internal/debug/wishes/{wishId}")
    fun get(@PathVariable wishId: UUID, @RequestHeader("X-Internal-Token", required = false) token: String?): DebugWishResponse {
        guard(token)
        return read(wishId)
    }

    @PostMapping("/internal/debug/wishes/{wishId}/fragments")
    fun set(@PathVariable wishId: UUID, @RequestBody request: DebugFragmentsRequest, @RequestHeader("X-Internal-Token", required = false) token: String?): DebugWishResponse {
        guard(token)
        val before = read(wishId)
        val earned = request.earnedFragments.coerceIn(0, before.requiredFragments)
        val order = jdbc.sql("select coalesce(fragment_mask_json -> 'revealOrder', '[]'::jsonb)::text from wish where id=:id").param("id", wishId).query(String::class.java).single()
        val indexes = mapper.readTree(order).mapIndexedNotNull { index, node -> if (index < earned) node.asInt() else null }
        val audit = audit(before, "debug.wish_fragments_set", earned)
        jdbc.sql("update wish set earned_fragments=:earned, fragment_lit_json=cast(:lit as jsonb), status=case when status='unlocked' and :earned < required_fragments then 'active' else status end, updated_at=now() where id=:id")
            .param("earned", earned).param("lit", mapper.writeValueAsString(mapOf("version" to 1, "litIndexes" to indexes))).param("id", wishId).update()
        return read(wishId).copy(auditId = audit)
    }

    @PostMapping("/internal/debug/wishes/{wishId}/unlock")
    @Transactional
    fun unlock(@PathVariable wishId: UUID, @RequestHeader("X-Internal-Token", required = false) token: String?): DebugWishResponse {
        guard(token)
        val before = read(wishId)
        if (before.status == "unlocked" || before.status == "redeemed") {
            return before.copy(auditId = existingAuditId(wishId))
        }
        val audit = audit(before, "debug.wish_unlock", before.requiredFragments)
        val order = jdbc.sql("select coalesce(fragment_mask_json -> 'revealOrder', '[]'::jsonb)::text from wish where id=:id").param("id", wishId).query(String::class.java).single()
        val parsedOrder = mapper.readTree(order)
        val revealOrderNode = parsedOrder["revealOrder"]
        val nodes = when {
            parsedOrder.isArray -> parsedOrder.mapNotNull { it.takeIf { node -> node.isInt || node.isLong }?.asInt() }
            parsedOrder.isObject && revealOrderNode?.isArray == true -> revealOrderNode.mapNotNull { it.takeIf { node -> node.isInt || node.isLong }?.asInt() }
            else -> emptyList()
        }
        jdbc.sql("update wish set earned_fragments=required_fragments, fragment_lit_json=cast(:lit as jsonb), status='unlocked', unlocked_at=coalesce(unlocked_at,now()), updated_at=now() where id=:id").param("lit", mapper.writeValueAsString(mapOf("version" to 1, "litIndexes" to nodes))).param("id", wishId).update()
        val after = read(wishId)
        if (before.status != "unlocked") events.publishFamilyEvent(after.familyId, "wish.unlocked", "wish", wishId, mapper.writeValueAsString(mapOf("wishId" to wishId, "familyId" to after.familyId, "childId" to after.childId, "earnedFragments" to after.earnedFragments, "requiredFragments" to after.requiredFragments)))
        return after.copy(auditId = audit)
    }

    private fun guard(token: String?) { if (!enabled) { log.warn("Rejected disabled debug endpoint request"); throw NotFoundError("Debug endpoints are disabled.") }; auth.requireToken(token) }
    private fun read(id: UUID): DebugWishResponse = jdbc.sql("select id,family_id,child_id,earned_fragments,required_fragments,status,fragment_mask_json::text from wish where id=:id").param("id", id).query { rs, _ -> DebugWishResponse(id, rs.getObject("family_id", UUID::class.java), rs.getObject("child_id", UUID::class.java), rs.getInt("earned_fragments"), rs.getInt("required_fragments"), rs.getString("status"), rs.getString("fragment_mask_json")) }.optional().orElseThrow { NotFoundError("Wish not found.") }
    private fun audit(before: DebugWishResponse, action: String, after: Int): UUID { val id=UUID.randomUUID(); jdbc.sql("insert into audit_log(id,family_id,actor_role,action,resource_type,resource_id,metadata_json) values(:id,(select family_id from wish where id=:wish),'admin_support',:action,'wish',:wish,cast(:meta as jsonb))").param("id",id).param("wish",before.wishId).param("action",action).param("meta",mapper.writeValueAsString(mapOf("beforeEarnedFragments" to before.earnedFragments,"afterEarnedFragments" to after,"beforeStatus" to before.status))).update(); return id }
    private fun existingAuditId(wishId: UUID): UUID? = jdbc.sql("select id from audit_log where action='debug.wish_unlock' and resource_id=:wish order by created_at desc limit 1").param("wish", wishId).query(UUID::class.java).optional().orElse(null)
}
