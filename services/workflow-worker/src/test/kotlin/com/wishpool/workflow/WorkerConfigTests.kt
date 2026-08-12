package com.wishpool.workflow

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkerConfigTests {
    @Test
    fun `configuration is loaded from environment map`() {
        val config = WorkerConfig.fromEnv(
            mapOf(
                "WISHPOOL_CORE_API_BASE_URL" to "http://core-api:8080",
                "WISHPOOL_INTERNAL_TOKEN" to "internal",
                "WISHPOOL_TEMPORAL_TARGET" to "temporal:7233",
                "WISHPOOL_TEMPORAL_TASK_QUEUE" to "queue",
                "WISHPOOL_OUTBOX_POLL_INTERVAL_MS" to "2500",
                "WISHPOOL_OUTBOX_CLAIM_LIMIT" to "25",
                "WISHPOOL_OUTBOX_RETRY_DELAY_SECONDS" to "120",
            ),
        )

        assertEquals("http://core-api:8080", config.coreApiBaseUrl.toString())
        assertEquals("internal", config.internalToken)
        assertEquals("temporal:7233", config.temporalTarget)
        assertEquals("queue", config.temporalTaskQueue)
        assertEquals(2500, config.outboxPollInterval.toMillis())
        assertEquals(25, config.outboxClaimLimit)
        assertEquals(120, config.outboxRetryDelaySeconds)
    }
}
