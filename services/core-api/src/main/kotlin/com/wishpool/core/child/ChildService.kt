package com.wishpool.core.child

import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import com.wishpool.core.shared.nullableInt
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.util.UUID

@Service
class ChildService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val policy: FamilyPolicy,
) {
    fun listChildren(familyId: UUID): List<ChildProfileResponse> {
        policy.requireMember(currentUser.require(), familyId)
        return jdbcClient.sql(
            """
            select id, family_id, nickname, birth_year, avatar_asset, room_theme, status
            from child_profile
            where family_id = :family_id
              and status = 'active'
            order by created_at
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .query(::childProfileResponse)
            .list()
    }

    @Transactional
    fun createChild(familyId: UUID, request: CreateChildRequest): ChildProfileResponse {
        policy.requireParent(currentUser.require(), familyId)
        validateBirthYear(request.birthYear)
        return jdbcClient.sql(
            """
            insert into child_profile (
              family_id, nickname, birth_year, avatar_asset, room_theme
            ) values (
              :family_id, :nickname, :birth_year, :avatar_asset, :room_theme
            )
            returning id, family_id, nickname, birth_year, avatar_asset, room_theme, status
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("nickname", request.nickname.trim())
            .param("birth_year", request.birthYear)
            .param("avatar_asset", request.avatarAsset)
            .param("room_theme", request.roomTheme ?: "star_cabin")
            .query(::childProfileResponse)
            .single()
    }

    @Transactional
    fun updateChild(childId: UUID, request: UpdateChildRequest): ChildProfileResponse {
        val user = currentUser.require()
        val member = policy.requireCanAccessChild(user, childId)
        if (member.role == "child_device") throw BadRequestError("Child devices cannot update child profiles.")
        validateBirthYear(request.birthYear)

        val current = findChild(childId) ?: throw NotFoundError("Child profile not found.")
        return jdbcClient.sql(
            """
            update child_profile
            set nickname = :nickname,
                birth_year = :birth_year,
                avatar_asset = :avatar_asset,
                room_theme = :room_theme,
                updated_at = now()
            where id = :id
            returning id, family_id, nickname, birth_year, avatar_asset, room_theme, status
            """.trimIndent(),
        )
            .param("id", childId)
            .param("nickname", request.nickname?.trim() ?: current.nickname)
            .param("birth_year", request.birthYear ?: current.birthYear)
            .param("avatar_asset", request.avatarAsset ?: current.avatarAsset)
            .param("room_theme", request.roomTheme ?: current.roomTheme)
            .query(::childProfileResponse)
            .single()
    }

    fun findChild(childId: UUID): ChildProfileResponse? =
        jdbcClient.sql(
            """
            select id, family_id, nickname, birth_year, avatar_asset, room_theme, status
            from child_profile
            where id = :id
            """.trimIndent(),
        )
            .param("id", childId)
            .query(::childProfileResponse)
            .optional()
            .orElse(null)

    private fun validateBirthYear(birthYear: Int?) {
        if (birthYear != null && birthYear !in 2000..2100) {
            throw BadRequestError("birthYear must be between 2000 and 2100.")
        }
    }
}

fun childProfileResponse(rs: ResultSet, rowNum: Int): ChildProfileResponse =
    ChildProfileResponse(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        nickname = rs.getString("nickname"),
        birthYear = rs.nullableInt("birth_year"),
        avatarAsset = rs.getString("avatar_asset"),
        roomTheme = rs.getString("room_theme"),
        status = rs.getString("status"),
    )
