package com.wishpool.admin

import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class AdminConfig(
    val port: Int,
    val coreApiBaseUrl: URI,
    val internalToken: String,
    val adminToken: String,
    val requestTimeout: Duration,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): AdminConfig =
            AdminConfig(
                port = (env["PORT"] ?: "8083").toInt(),
                coreApiBaseUrl = normalizedUri(env["WISHPOOL_CORE_API_BASE_URL"] ?: "http://localhost:8080"),
                internalToken = env["WISHPOOL_INTERNAL_TOKEN"] ?: "wishpool-local-internal-token",
                adminToken = env["WISHPOOL_ADMIN_TOKEN"] ?: "wishpool-local-admin-token",
                requestTimeout = (env["WISHPOOL_ADMIN_REQUEST_TIMEOUT_MS"] ?: "5000").toLong()
                    .coerceIn(500, 30000)
                    .milliseconds,
            )

        private fun normalizedUri(value: String): URI {
            val normalized = if (value.endsWith("/")) value else "$value/"
            return URI.create(normalized)
        }
    }
}
