package com.wishpool.core.auth

import com.wishpool.core.family.FamilyMemberResponse
import com.wishpool.core.family.FamilyResponse
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.security.TokenService
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.Hashing
import com.wishpool.core.shared.UnauthorizedError
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@Service
class AuthService(
    private val jdbcClient: JdbcClient,
    private val hashing: Hashing,
    private val tokenService: TokenService,
    private val currentUser: CurrentUser,
    private val clock: Clock,
    @Value("\${wishpool.auth.access-token-ttl-sec}") private val accessTokenTtlSec: Long,
    @Value("\${wishpool.auth.refresh-token-ttl-sec}") private val refreshTokenTtlSec: Long,
    @Value("\${wishpool.auth.phone-code-ttl-sec}") private val phoneCodeTtlSec: Long,
    @Value("\${wishpool.auth.local-debug-phone-code}") private val localDebugPhoneCode: Boolean,
) {
    @Transactional
    fun requestPhoneCode(request: PhoneCodeRequest): PhoneCodeCreated {
        if (request.purpose != "login") throw BadRequestError("Unsupported phone code purpose.")
        val phoneNumber = normalizePhone(request.phoneNumber)
        val token = hashing.randomToken()
        val code = hashing.numericCode()
        val expiresAt = OffsetDateTime.ofInstant(clock.instant().plusSeconds(phoneCodeTtlSec), ZoneOffset.UTC)

        jdbcClient.sql(
            """
            insert into phone_verification_code (
              phone_number, purpose, verification_token_hash, code_hash, expires_at
            ) values (
              :phone_number, :purpose, :verification_token_hash, :code_hash, :expires_at
            )
            """.trimIndent(),
        )
            .param("phone_number", phoneNumber)
            .param("purpose", request.purpose)
            .param("verification_token_hash", hashing.sha256(token))
            .param("code_hash", hashing.sha256(code))
            .param("expires_at", expiresAt)
            .update()

        return PhoneCodeCreated(
            verificationToken = token,
            expiresAt = expiresAt,
            debugCode = code.takeIf { localDebugPhoneCode },
        )
    }

    @Transactional
    fun login(request: LoginRequest): AuthTokenPair {
        val user = when (request.provider) {
            "phone" -> loginByPhone(request.credential)
            "wechat" -> throw BadRequestError("WeChat login provider is reserved but not configured.")
            else -> throw BadRequestError("Unsupported login provider.")
        }

        jdbcClient.sql("update auth_user set last_login_at = now(), updated_at = now() where id = :id")
            .param("id", user.id)
            .update()

        val deviceId = request.device?.let { upsertDevice(user.id, null, null, it) }
        return issueTokens(user.id, deviceId)
    }

    @Transactional
    fun refresh(request: RefreshRequest): AuthTokenPair {
        val refreshTokenHash = hashing.sha256(request.refreshToken)
        val session = jdbcClient.sql(
            """
            select id, user_id, device_id
            from auth_session
            where refresh_token_hash = :hash
              and status = 'active'
              and expires_at > now()
            """.trimIndent(),
        )
            .param("hash", refreshTokenHash)
            .query { rs, _ ->
                SessionRecord(
                    id = rs.getObject("id", UUID::class.java),
                    userId = rs.getObject("user_id", UUID::class.java),
                    deviceId = rs.getObject("device_id", UUID::class.java),
                )
            }
            .optional()
            .orElseThrow { UnauthorizedError("Refresh token is invalid or expired.") }

        jdbcClient.sql("update auth_session set status = 'revoked', revoked_at = now() where id = :id")
            .param("id", session.id)
            .update()

        return issueTokens(session.userId, session.deviceId)
    }

    fun me(): MeResponse {
        val user = currentUser.require()
        return MeResponse(
            user = findUser(user.userId) ?: throw UnauthorizedError("Authenticated user no longer exists."),
            families = familyContexts(user.userId),
        )
    }

    fun issueChildDeviceTokens(
        userId: UUID,
        familyId: UUID,
        childId: UUID,
        device: DeviceRegistration,
    ): AuthTokenPair {
        val deviceId = upsertDevice(userId, familyId, childId, device)
        return issueTokens(userId, deviceId)
    }

    private fun loginByPhone(credential: String): UserRecord {
        val parts = credential.split(":", limit = 2)
        if (parts.size != 2) throw BadRequestError("Phone credential must use verificationToken:code.")
        val tokenHash = hashing.sha256(parts[0])
        val codeHash = hashing.sha256(parts[1])

        val verification = jdbcClient.sql(
            """
            select id, phone_number, code_hash, attempt_count
            from phone_verification_code
            where verification_token_hash = :token_hash
              and purpose = 'login'
              and status = 'active'
              and expires_at > now()
            """.trimIndent(),
        )
            .param("token_hash", tokenHash)
            .query { rs, _ ->
                PhoneVerificationRecord(
                    id = rs.getObject("id", UUID::class.java),
                    phoneNumber = rs.getString("phone_number"),
                    codeHash = rs.getString("code_hash"),
                    attemptCount = rs.getInt("attempt_count"),
                )
            }
            .optional()
            .orElseThrow { UnauthorizedError("Phone code is invalid or expired.") }

        if (verification.attemptCount >= 5) {
            expirePhoneCode(verification.id)
            throw UnauthorizedError("Phone code attempt limit exceeded.")
        }

        if (verification.codeHash != codeHash) {
            jdbcClient.sql("update phone_verification_code set attempt_count = attempt_count + 1 where id = :id")
                .param("id", verification.id)
                .update()
            throw UnauthorizedError("Phone code is invalid.")
        }

        jdbcClient.sql(
            """
            update phone_verification_code
            set status = 'consumed', consumed_at = now()
            where id = :id and status = 'active'
            """.trimIndent(),
        )
            .param("id", verification.id)
            .update()
            .also { rows ->
                if (rows != 1) throw ConflictError("Phone code has already been consumed.")
            }

        return findOrCreatePhoneUser(verification.phoneNumber)
    }

    private fun findOrCreatePhoneUser(phoneNumber: String): UserRecord {
        val existing = jdbcClient.sql(
            """
            select u.id, u.status, u.display_name, u.avatar_url
            from auth_provider_identity api
            join auth_user u on u.id = api.user_id
            where api.provider = 'phone'
              and api.provider_subject = :phone
            """.trimIndent(),
        )
            .param("phone", phoneNumber)
            .query(::userRecord)
            .optional()
            .orElse(null)

        if (existing != null) return existing

        val displayName = "家长${phoneNumber.takeLast(4)}"
        val userId = jdbcClient.sql(
            """
            insert into auth_user (status, display_name)
            values ('active', :display_name)
            returning id
            """.trimIndent(),
        )
            .param("display_name", displayName)
            .query(UUID::class.java)
            .single()

        jdbcClient.sql(
            """
            insert into auth_provider_identity (user_id, provider, provider_subject, verified_at)
            values (:user_id, 'phone', :phone, now())
            """.trimIndent(),
        )
            .param("user_id", userId)
            .param("phone", phoneNumber)
            .update()

        return findUserRecord(userId) ?: error("Created user was not found.")
    }

    private fun issueTokens(userId: UUID, deviceId: UUID?): AuthTokenPair {
        val refreshToken = hashing.randomToken()
        val expiresAt = OffsetDateTime.ofInstant(clock.instant().plusSeconds(refreshTokenTtlSec), ZoneOffset.UTC)
        val sessionId = jdbcClient.sql(
            """
            insert into auth_session (user_id, device_id, refresh_token_hash, expires_at)
            values (:user_id, :device_id, :refresh_token_hash, :expires_at)
            returning id
            """.trimIndent(),
        )
            .param("user_id", userId)
            .param("device_id", deviceId)
            .param("refresh_token_hash", hashing.sha256(refreshToken))
            .param("expires_at", expiresAt)
            .query(UUID::class.java)
            .single()

        val accessToken = tokenService.createAccessToken(userId, sessionId, deviceId, accessTokenTtlSec)
        val user = findUser(userId) ?: throw UnauthorizedError("Authenticated user no longer exists.")

        return AuthTokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSec = accessTokenTtlSec,
            user = user,
            primaryFamilyId = primaryFamilyId(userId),
        )
    }

    private fun upsertDevice(
        userId: UUID,
        familyId: UUID?,
        childId: UUID?,
        device: DeviceRegistration,
    ): UUID =
        jdbcClient.sql(
            """
            insert into device (
              user_id, family_id, child_id, platform, device_name, push_provider, push_token, last_seen_at
            ) values (
              :user_id, :family_id, :child_id, :platform, :device_name, :push_provider, :push_token, now()
            )
            returning id
            """.trimIndent(),
        )
            .param("user_id", userId)
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("platform", device.platform)
            .param("device_name", device.deviceName)
            .param("push_provider", device.pushProvider)
            .param("push_token", device.pushToken)
            .query(UUID::class.java)
            .single()

    private fun expirePhoneCode(id: UUID) {
        jdbcClient.sql("update phone_verification_code set status = 'expired' where id = :id")
            .param("id", id)
            .update()
    }

    private fun findUser(id: UUID): UserResponse? =
        findUserRecord(id)?.toResponse()

    private fun findUserRecord(id: UUID): UserRecord? =
        jdbcClient.sql("select id, status, display_name, avatar_url from auth_user where id = :id")
            .param("id", id)
            .query(::userRecord)
            .optional()
            .orElse(null)

    private fun primaryFamilyId(userId: UUID): UUID? =
        jdbcClient.sql(
            """
            select family_id
            from family_member
            where user_id = :user_id
              and status = 'active'
            order by case role when 'parent_owner' then 0 when 'parent' then 1 else 2 end, created_at
            limit 1
            """.trimIndent(),
        )
            .param("user_id", userId)
            .query(UUID::class.java)
            .optional()
            .orElse(null)

    private fun familyContexts(userId: UUID): List<FamilyMemberContextResponse> =
        jdbcClient.sql(
            """
            select
              f.id as family_id,
              f.name as family_name,
              f.timezone,
              f.status as family_status,
              fm.id as member_id,
              fm.user_id,
              fm.role,
              fm.child_id,
              fm.display_name,
              fm.status as member_status
            from family_member fm
            join family f on f.id = fm.family_id
            where fm.user_id = :user_id
              and fm.status = 'active'
            order by f.created_at
            """.trimIndent(),
        )
            .param("user_id", userId)
            .query { rs, _ ->
                FamilyMemberContextResponse(
                    family = FamilyResponse(
                        id = rs.getObject("family_id", UUID::class.java),
                        name = rs.getString("family_name"),
                        timezone = rs.getString("timezone"),
                        status = rs.getString("family_status"),
                    ),
                    member = FamilyMemberResponse(
                        id = rs.getObject("member_id", UUID::class.java),
                        familyId = rs.getObject("family_id", UUID::class.java),
                        userId = rs.getObject("user_id", UUID::class.java),
                        role = rs.getString("role"),
                        childId = rs.getObject("child_id", UUID::class.java),
                        displayName = rs.getString("display_name"),
                        status = rs.getString("member_status"),
                    ),
                )
            }
            .list()

    private fun normalizePhone(phoneNumber: String): String =
        phoneNumber.trim().replace(" ", "").replace("-", "").lowercase(Locale.ROOT)
}

data class UserRecord(
    val id: UUID,
    val status: String,
    val displayName: String,
    val avatarUrl: String?,
) {
    fun toResponse(): UserResponse =
        UserResponse(
            id = id,
            status = status,
            displayName = displayName,
            avatarUrl = avatarUrl,
        )
}

data class SessionRecord(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID?,
)

data class PhoneVerificationRecord(
    val id: UUID,
    val phoneNumber: String,
    val codeHash: String,
    val attemptCount: Int,
)

fun userRecord(rs: ResultSet, rowNum: Int): UserRecord =
    UserRecord(
        id = rs.getObject("id", UUID::class.java),
        status = rs.getString("status"),
        displayName = rs.getString("display_name"),
        avatarUrl = rs.getString("avatar_url"),
    )
