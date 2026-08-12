package com.wishpool.workflow

import java.net.URI
import java.time.Duration

data class WorkerConfig(
    val coreApiBaseUrl: URI,
    val internalToken: String,
    val temporalTarget: String,
    val temporalTaskQueue: String,
    val outboxPollInterval: Duration,
    val outboxClaimLimit: Int,
    val outboxRetryDelaySeconds: Long,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): WorkerConfig =
            WorkerConfig(
                coreApiBaseUrl = URI.create(env["WISHPOOL_CORE_API_BASE_URL"] ?: "http://localhost:8080"),
                internalToken = env["WISHPOOL_INTERNAL_TOKEN"] ?: "wishpool-local-internal-token",
                temporalTarget = env["WISHPOOL_TEMPORAL_TARGET"] ?: "localhost:7233",
                temporalTaskQueue = env["WISHPOOL_TEMPORAL_TASK_QUEUE"] ?: "wishpool-workflows",
                outboxPollInterval = Duration.ofMillis((env["WISHPOOL_OUTBOX_POLL_INTERVAL_MS"] ?: "1000").toLong()),
                outboxClaimLimit = (env["WISHPOOL_OUTBOX_CLAIM_LIMIT"] ?: "50").toInt(),
                outboxRetryDelaySeconds = (env["WISHPOOL_OUTBOX_RETRY_DELAY_SECONDS"] ?: "60").toLong(),
            )
    }
}
