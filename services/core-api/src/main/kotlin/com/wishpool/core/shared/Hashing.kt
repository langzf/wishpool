package com.wishpool.core.shared

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HexFormat
import java.util.UUID

@Component
class Hashing {
    private val random = SecureRandom()

    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }

    fun randomToken(): String = UUID.randomUUID().toString() + "." + randomHex(32)

    fun numericCode(length: Int = 6): String =
        buildString {
            repeat(length) {
                append(random.nextInt(10))
            }
        }

    private fun randomHex(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        random.nextBytes(bytes)
        return HexFormat.of().formatHex(bytes)
    }
}
