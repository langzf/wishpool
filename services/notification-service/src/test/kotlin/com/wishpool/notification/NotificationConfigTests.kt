package com.wishpool.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationConfigTests {
    @Test
    fun `loads local defaults`() {
        val config = NotificationConfig.fromEnv(emptyMap())

        assertEquals(8084, config.port)
        assertEquals("http://localhost:8080/", config.coreApiBaseUrl.toString())
        assertEquals("wishpool-local-internal-token", config.internalToken)
        assertEquals(1000, config.pollInterval.inWholeMilliseconds)
        assertEquals(50, config.claimLimit)
        assertFalse(config.dispatchEnabled)
    }

    @Test
    fun `honors overrides and clamps polling`() {
        val config = NotificationConfig.fromEnv(
            mapOf(
                "PORT" to "18084",
                "WISHPOOL_CORE_API_BASE_URL" to "http://core-api:8080/api",
                "WISHPOOL_INTERNAL_TOKEN" to "secret",
                "WISHPOOL_NOTIFICATION_POLL_INTERVAL_MS" to "100",
                "WISHPOOL_NOTIFICATION_CLAIM_LIMIT" to "500",
                "WISHPOOL_NOTIFICATION_DISPATCH_ENABLED" to "true",
            ),
        )

        assertEquals(18084, config.port)
        assertEquals("http://core-api:8080/api/", config.coreApiBaseUrl.toString())
        assertEquals("secret", config.internalToken)
        assertEquals(250, config.pollInterval.inWholeMilliseconds)
        assertEquals(100, config.claimLimit)
        assertTrue(config.dispatchEnabled)
    }
}
