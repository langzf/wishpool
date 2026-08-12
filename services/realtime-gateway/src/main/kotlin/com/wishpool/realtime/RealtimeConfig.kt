package com.wishpool.realtime

import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class RealtimeConfig(
    val port: Int,
    val coreApiBaseUrl: URI,
    val redisUri: String,
    val redisEnabled: Boolean,
    val pollInterval: Duration,
    val heartbeatInterval: Duration,
    val syncLimit: Int,
    val connectionTtlSeconds: Long,
    val httpTimeoutSeconds: Long,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): RealtimeConfig {
            val coreApiBaseUrl = env["WISHPOOL_CORE_API_BASE_URL"] ?: "http://localhost:8080"
            return RealtimeConfig(
                port = env["WISHPOOL_REALTIME_PORT"]?.toIntOrNull()
                    ?: env["PORT"]?.toIntOrNull()
                    ?: 8081,
                coreApiBaseUrl = URI.create(coreApiBaseUrl.trimEnd('/') + "/"),
                redisUri = env["WISHPOOL_REDIS_URI"]
                    ?: env["REDIS_URL"]
                    ?: "redis://localhost:6379",
                redisEnabled = env["WISHPOOL_REDIS_ENABLED"]?.toBooleanStrictOrNull() ?: true,
                pollInterval = (env["WISHPOOL_REALTIME_POLL_INTERVAL_MS"]?.toLongOrNull() ?: 1000L)
                    .coerceAtLeast(250L)
                    .milliseconds,
                heartbeatInterval = (env["WISHPOOL_REALTIME_HEARTBEAT_INTERVAL_SECONDS"]?.toLongOrNull() ?: 15L)
                    .coerceAtLeast(5L)
                    .seconds,
                syncLimit = (env["WISHPOOL_REALTIME_SYNC_LIMIT"]?.toIntOrNull() ?: 500)
                    .coerceIn(1, 1000),
                connectionTtlSeconds = (env["WISHPOOL_REALTIME_CONNECTION_TTL_SECONDS"]?.toLongOrNull() ?: 90L)
                    .coerceAtLeast(30L),
                httpTimeoutSeconds = (env["WISHPOOL_REALTIME_HTTP_TIMEOUT_SECONDS"]?.toLongOrNull() ?: 10L)
                    .coerceAtLeast(2L),
            )
        }
    }
}
