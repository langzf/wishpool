package com.wishpool.core.security

import com.wishpool.core.shared.BadRequestError
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.HexFormat
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class TokenService(
    @Value("\${wishpool.auth.token-secret}") private val tokenSecret: String,
    private val clock: Clock,
) {
    fun createAccessToken(
        userId: UUID,
        sessionId: UUID,
        deviceId: UUID?,
        ttlSeconds: Long,
    ): String {
        val expiresAt = clock.instant().plusSeconds(ttlSeconds).epochSecond
        val payload = listOf(
            "v1",
            userId,
            sessionId,
            deviceId ?: "",
            expiresAt,
        ).joinToString("|")
        val encodedPayload = base64Url(payload)
        val signature = sign(encodedPayload)
        return "$encodedPayload.$signature"
    }

    fun parseAccessToken(token: String): AccessTokenClaims {
        val parts = token.split(".")
        if (parts.size != 2) throw BadRequestError("Invalid access token.")
        val expected = sign(parts[0])
        if (!MessageDigest.isEqual(expected.toByteArray(), parts[1].toByteArray())) {
            throw BadRequestError("Invalid access token signature.")
        }
        val payload = String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
        val fields = payload.split("|")
        if (fields.size != 5 || fields[0] != "v1") throw BadRequestError("Invalid access token payload.")
        val expiresAt = Instant.ofEpochSecond(fields[4].toLong())
        if (expiresAt.isBefore(clock.instant())) throw BadRequestError("Access token expired.")
        return AccessTokenClaims(
            userId = UUID.fromString(fields[1]),
            sessionId = UUID.fromString(fields[2]),
            deviceId = fields[3].takeIf { it.isNotBlank() }?.let(UUID::fromString),
            expiresAt = expiresAt,
        )
    }

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(tokenSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return HexFormat.of().formatHex(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun base64Url(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
}

data class AccessTokenClaims(
    val userId: UUID,
    val sessionId: UUID,
    val deviceId: UUID?,
    val expiresAt: Instant,
)
