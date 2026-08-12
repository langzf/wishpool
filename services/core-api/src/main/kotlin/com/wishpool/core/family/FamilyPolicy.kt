package com.wishpool.core.family

import com.wishpool.core.security.AuthenticatedUser
import com.wishpool.core.shared.ForbiddenError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FamilyPolicy(
    private val jdbcClient: JdbcClient,
) {
    fun requireParent(user: AuthenticatedUser, familyId: UUID): FamilyMemberRecord {
        val member = memberForUser(user.userId, familyId)
            ?: throw ForbiddenError("User is not a member of this family.")
        if (member.role !in setOf("parent_owner", "parent")) {
            throw ForbiddenError("Parent permission is required.")
        }
        return member
    }

    fun requireMember(user: AuthenticatedUser, familyId: UUID): FamilyMemberRecord =
        memberForUser(user.userId, familyId)
            ?: throw ForbiddenError("User is not a member of this family.")

    fun requireCanAccessChild(user: AuthenticatedUser, childId: UUID): FamilyMemberRecord {
        val member = jdbcClient.sql(
            """
            select fm.id, fm.family_id, fm.user_id, fm.role, fm.child_id, fm.display_name, fm.status
            from family_member fm
            join child_profile cp on cp.family_id = fm.family_id
            where fm.user_id = :user_id
              and cp.id = :child_id
              and fm.status = 'active'
              and (
                fm.role in ('parent_owner', 'parent')
                or (fm.role = 'child_device' and fm.child_id = cp.id)
              )
            """.trimIndent(),
        )
            .param("user_id", user.userId)
            .param("child_id", childId)
            .query(::familyMemberRecord)
            .optional()
            .orElse(null)

        return member ?: throw ForbiddenError("User cannot access this child profile.")
    }

    fun memberForUser(userId: UUID, familyId: UUID): FamilyMemberRecord? =
        jdbcClient.sql(
            """
            select id, family_id, user_id, role, child_id, display_name, status
            from family_member
            where user_id = :user_id
              and family_id = :family_id
              and status = 'active'
            """.trimIndent(),
        )
            .param("user_id", userId)
            .param("family_id", familyId)
            .query(::familyMemberRecord)
            .optional()
            .orElse(null)
}

data class FamilyMemberRecord(
    val id: UUID,
    val familyId: UUID,
    val userId: UUID,
    val role: String,
    val childId: UUID?,
    val displayName: String,
    val status: String,
)

fun familyMemberRecord(rs: java.sql.ResultSet, rowNum: Int): FamilyMemberRecord =
    FamilyMemberRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        role = rs.getString("role"),
        childId = rs.getObject("child_id", UUID::class.java),
        displayName = rs.getString("display_name"),
        status = rs.getString("status"),
    )
