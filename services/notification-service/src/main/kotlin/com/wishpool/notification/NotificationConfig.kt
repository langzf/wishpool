package com.wishpool.notification

import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class NotificationConfig(
    val port: Int,
    val coreApiBaseUrl: URI,
    val internalToken: String,
    val pollInterval: Duration,
    val claimLimit: Int,
    val dispatchEnabled: Boolean,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): NotificationConfig =
            NotificationConfig(
                port = (env["PORT"] ?: "8084").toInt(),
                coreApiBaseUrl = normalizedUri(env["WISHPOOL_CORE_API_BASE_URL"] ?: "http://localhost:8080"),
                internalToken = env["WISHPOOL_INTERNAL_TOKEN"] ?: "wishpool-local-internal-token",
                pollInterval = (env["WISHPOOL_NOTIFICATION_POLL_INTERVAL_MS"] ?: "1000").toLong()
                    .coerceIn(250, 60000)
                    .milliseconds,
                claimLimit = (env["WISHPOOL_NOTIFICATION_CLAIM_LIMIT"] ?: "50").toInt().coerceIn(1, 100),
                dispatchEnabled = (env["WISHPOOL_NOTIFICATION_DISPATCH_ENABLED"] ?: "false").toBoolean(),
            )

        private fun normalizedUri(value: String): URI {
            val normalized = if (value.endsWith("/")) value else "$value/"
            return URI.create(normalized)
        }
    }
}
