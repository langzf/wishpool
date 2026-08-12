package com.wishpool.core.pairing

import com.wishpool.core.auth.AuthService
import com.wishpool.core.auth.AuthTokenPair
import com.wishpool.core.child.ChildService
import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.Hashing
import com.wishpool.core.shared.NotFoundError
import com.wishpool.core.shared.UnauthorizedError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

@Service
class PairingService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val policy: FamilyPolicy,
    private val childService: ChildService,
    private val authService: AuthService,
    private val hashing: Hashing,
    private val eventPublisher: DomainEventPublisher,
) {
    @Transactional
    fun createPairingSession(
        familyId: UUID,
        request: CreatePairingSessionRequest,
    ): PairingSessionCreated {
        val user = currentUser.require()
        policy.requireParent(user, familyId)
        val child = childService.findChild(request.childId)
            ?: throw NotFoundError("Child profile not found.")
        if (child.familyId != familyId) throw BadRequestError("Child profile does not belong to this family.")

        val pairingCode = generatePairingCode()
        val expiresAt = OffsetDateTime.now().plusMinutes(15)
        val session = jdbcClient.sql(
            """
            insert into pairing_session (
              family_id, child_id, created_by, code_hash, expires_at
            ) values (
              :family_id, :child_id, :created_by, :code_hash, :expires_at
            )
            returning id, expires_at
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", request.childId)
            .param("created_by", user.userId)
            .param("code_hash", hashing.sha256(pairingCode))
            .param("expires_at", expiresAt)
            .query { rs, _ ->
                PairingSessionCreated(
                    sessionId = rs.getObject("id", UUID::class.java),
                    pairingCode = pairingCode,
                    expiresAt = rs.getObject("expires_at", OffsetDateTime::class.java),
                )
            }
            .single()

        audit(familyId, user.userId, "pairing.create", "pairing_session", session.sessionId)
        return session
    }

    @Transactional
    fun consumePairingCode(request: ConsumePairingCodeRequest): AuthTokenPair {
        val codeHash = hashing.sha256(normalizeCode(request.pairingCode))
        val pairing = jdbcClient.sql(
            """
            select id, family_id, child_id
            from pairing_session
            where code_hash = :code_hash
              and status = 'active'
              and expires_at > now()
            order by created_at desc
            limit 1
            """.trimIndent(),
        )
            .param("code_hash", codeHash)
            .query(::pairingRecord)
            .optional()
            .orElseThrow { UnauthorizedError("Pairing code is invalid or expired.") }

        val child = childService.findChild(pairing.childId)
            ?: throw NotFoundError("Child profile not found.")

        val childUserId = jdbcClient.sql(
            """
            insert into auth_user (status, display_name)
            values ('active', :display_name)
            returning id
            """.trimIndent(),
        )
            .param("display_name", "${child.nickname}的设备")
            .query(UUID::class.java)
            .single()

        jdbcClient.sql(
            """
            insert into family_member (
              family_id, user_id, role, child_id, display_name, status
            ) values (
              :family_id, :user_id, 'child_device', :child_id, :display_name, 'active'
            )
            """.trimIndent(),
        )
            .param("family_id", pairing.familyId)
            .param("user_id", childUserId)
            .param("child_id", pairing.childId)
            .param("display_name", child.nickname)
            .update()

        jdbcClient.sql(
            """
            update pairing_session
            set status = 'consumed',
                consumed_by_user_id = :user_id,
                consumed_at = now()
            where id = :id
              and status = 'active'
            """.trimIndent(),
        )
            .param("id", pairing.id)
            .param("user_id", childUserId)
            .update()
            .also { rows ->
                if (rows != 1) throw ConflictError("Pairing code has already been consumed.")
            }

        audit(pairing.familyId, childUserId, "pairing.consume", "pairing_session", pairing.id, actorRole = "child_device")
        eventPublisher.publishFamilyEvent(
            familyId = pairing.familyId,
            eventType = "family.child_device_paired",
            aggregateType = "pairing_session",
            aggregateId = pairing.id,
            payload = mapOf(
                "familyId" to pairing.familyId.toString(),
                "childId" to pairing.childId.toString(),
                "pairedUserId" to childUserId.toString(),
            ),
        )
        return authService.issueChildDeviceTokens(childUserId, pairing.familyId, pairing.childId, request.device)
    }

    private fun generatePairingCode(): String =
        buildString {
            repeat(3) {
                if (it > 0) append("-")
                append(hashing.numericCode(3))
            }
        }

    private fun normalizeCode(code: String): String =
        code.trim().replace(" ", "").uppercase(Locale.ROOT)

    private fun audit(
        familyId: UUID,
        userId: UUID,
        action: String,
        resourceType: String,
        resourceId: UUID,
        actorRole: String = "parent",
    ) {
        jdbcClient.sql(
            """
            insert into audit_log (
              family_id, actor_user_id, actor_role, action, resource_type, resource_id
            ) values (
              :family_id, :actor_user_id, :actor_role, :action, :resource_type, :resource_id
            )
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("actor_user_id", userId)
            .param("actor_role", actorRole)
            .param("action", action)
            .param("resource_type", resourceType)
            .param("resource_id", resourceId)
            .update()
    }
}

data class PairingRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
)

fun pairingRecord(rs: ResultSet, rowNum: Int): PairingRecord =
    PairingRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
    )
