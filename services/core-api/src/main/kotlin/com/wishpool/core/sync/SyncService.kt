package com.wishpool.core.sync

import com.wishpool.core.family.FamilyMemberRecord
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@Service
class SyncService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val objectMapper: ObjectMapper,
) {
    fun pullEvents(familyId: UUID, afterSeq: Long, requestedLimit: Int?): SyncPullResponse {
        if (afterSeq < 0) throw BadRequestError("afterSeq must be greater than or equal to 0.")
        val limit = requestedLimit?.coerceIn(1, MAX_LIMIT) ?: DEFAULT_LIMIT
        val member = familyPolicy.requireMember(currentUser.require(), familyId)
        val records = jdbcClient.sql(
            """
            select id, family_id, seq, event_type, aggregate_type, aggregate_id, payload_json::text, created_at
            from family_event
            where family_id = :family_id
              and seq > :after_seq
            order by seq
            limit cast(:limit as integer)
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("after_seq", afterSeq)
            .param("limit", limit)
            .query(::familyEventRecord)
            .list()

        val events = records.map { record ->
            if (record.visibleTo(member)) record.toResponse() else record.toRedactedResponse()
        }
        return SyncPullResponse(
            events = events,
            latestSeq = events.lastOrNull()?.seq ?: afterSeq,
        )
    }

    private fun familyEventRecord(rs: java.sql.ResultSet, rowNum: Int): FamilyEventRecord =
        FamilyEventRecord(
            id = rs.getObject("id", UUID::class.java),
            familyId = rs.getObject("family_id", UUID::class.java),
            seq = rs.getLong("seq"),
            type = rs.getString("event_type"),
            aggregateType = rs.getString("aggregate_type"),
            aggregateId = rs.getObject("aggregate_id", UUID::class.java),
            occurredAt = rs.getObject("created_at", OffsetDateTime::class.java),
            payload = objectMapper.readTree(rs.getString("payload_json")),
        )

    private fun FamilyEventRecord.visibleTo(member: FamilyMemberRecord): Boolean {
        if (member.role in setOf("parent_owner", "parent")) return true
        if (member.role != "child_device") return false
        val childId = member.childId ?: return false
        return payload.hasNonNull("childId") && payload.path("childId").asString() == childId.toString()
    }

    private fun FamilyEventRecord.toResponse(): FamilyEventResponse =
        FamilyEventResponse(
            seq = seq,
            familyId = familyId,
            type = type,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            occurredAt = occurredAt,
            payload = payload,
        )

    private fun FamilyEventRecord.toRedactedResponse(): FamilyEventResponse =
        FamilyEventResponse(
            seq = seq,
            familyId = familyId,
            type = "sync.redacted",
            aggregateType = "sync",
            aggregateId = id,
            occurredAt = occurredAt,
            payload = objectMapper.createObjectNode(),
        )

    private companion object {
        const val DEFAULT_LIMIT = 500
        const val MAX_LIMIT = 1000
    }
}
