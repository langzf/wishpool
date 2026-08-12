package com.wishpool.core.family

import com.wishpool.core.child.ChildProfileResponse
import com.wishpool.core.child.ChildService
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.Hashing
import com.wishpool.core.shared.NotFoundError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

@Service
class FamilyService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val policy: FamilyPolicy,
    private val childService: ChildService,
    private val hashing: Hashing,
) {
    @Transactional
    fun createFamily(request: CreateFamilyRequest): FamilyResponse {
        val user = currentUser.require()
        val family = jdbcClient.sql(
            """
            insert into family (name, timezone, owner_user_id)
            values (:name, :timezone, :owner_user_id)
            returning id, name, timezone, status
            """.trimIndent(),
        )
            .param("name", request.name.trim())
            .param("timezone", request.timezone)
            .param("owner_user_id", user.userId)
            .query(::familyResponse)
            .single()

        val displayName = jdbcClient.sql("select display_name from auth_user where id = :id")
            .param("id", user.userId)
            .query(String::class.java)
            .single()

        jdbcClient.sql(
            """
            insert into family_member (family_id, user_id, role, display_name, status)
            values (:family_id, :user_id, 'parent_owner', :display_name, 'active')
            """.trimIndent(),
        )
            .param("family_id", family.id)
            .param("user_id", user.userId)
            .param("display_name", displayName)
            .update()

        request.firstChild?.let { childService.createChild(family.id, it) }
        audit(family.id, user.userId, "family.create", "family", family.id)
        return family
    }

    fun getFamily(familyId: UUID): FamilyResponse {
        policy.requireMember(currentUser.require(), familyId)
        return jdbcClient.sql("select id, name, timezone, status from family where id = :id")
            .param("id", familyId)
            .query(::familyResponse)
            .optional()
            .orElseThrow { NotFoundError("Family not found.") }
    }

    fun listMembers(familyId: UUID): List<FamilyMemberResponse> {
        policy.requireMember(currentUser.require(), familyId)
        return jdbcClient.sql(
            """
            select id, family_id, user_id, role, child_id, display_name, status
            from family_member
            where family_id = :family_id
              and status != 'removed'
            order by created_at
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .query(::familyMemberResponse)
            .list()
    }

    @Transactional
    fun inviteParent(familyId: UUID, request: InviteParentRequest): FamilyInviteResponse {
        val user = currentUser.require()
        policy.requireParent(user, familyId)
        val token = hashing.randomToken()
        val invite = jdbcClient.sql(
            """
            insert into family_invite (
              family_id, invited_by, invitee_contact_hash, token_hash, expires_at
            ) values (
              :family_id, :invited_by, :contact_hash, :token_hash, now() + interval '7 days'
            )
            returning id, family_id, status, expires_at
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("invited_by", user.userId)
            .param("contact_hash", hashing.sha256(request.contact.trim()))
            .param("token_hash", hashing.sha256(token))
            .query(::familyInviteResponse)
            .single()
        audit(familyId, user.userId, "family.invite_parent", "family_invite", invite.id)
        return invite
    }

    fun childBelongsToFamily(childId: UUID, familyId: UUID): Boolean =
        jdbcClient.sql("select count(*) from child_profile where id = :child_id and family_id = :family_id")
            .param("child_id", childId)
            .param("family_id", familyId)
            .query(Int::class.java)
            .single() == 1

    private fun audit(familyId: UUID, userId: UUID, action: String, resourceType: String, resourceId: UUID) {
        jdbcClient.sql(
            """
            insert into audit_log (
              family_id, actor_user_id, actor_role, action, resource_type, resource_id
            ) values (
              :family_id, :actor_user_id, 'parent', :action, :resource_type, :resource_id
            )
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("actor_user_id", userId)
            .param("action", action)
            .param("resource_type", resourceType)
            .param("resource_id", resourceId)
            .update()
    }
}

fun familyResponse(rs: ResultSet, rowNum: Int): FamilyResponse =
    FamilyResponse(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        timezone = rs.getString("timezone"),
        status = rs.getString("status"),
    )

fun familyMemberResponse(rs: ResultSet, rowNum: Int): FamilyMemberResponse =
    FamilyMemberResponse(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        role = rs.getString("role"),
        childId = rs.getObject("child_id", UUID::class.java),
        displayName = rs.getString("display_name"),
        status = rs.getString("status"),
    )

fun familyInviteResponse(rs: ResultSet, rowNum: Int): FamilyInviteResponse =
    FamilyInviteResponse(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        status = rs.getString("status"),
        expiresAt = rs.getObject("expires_at", OffsetDateTime::class.java),
    )
