package com.wishpool.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RealtimeConfigTests {
    @Test
    fun `loads defaults for local runtime`() {
        val config = RealtimeConfig.fromEnv(emptyMap())

        assertEquals(8081, config.port)
        assertEquals("http://localhost:8080/", config.coreApiBaseUrl.toString())
        assertEquals("redis://localhost:6379", config.redisUri)
        assertEquals(1000.milliseconds, config.pollInterval)
        assertEquals(15.seconds, config.heartbeatInterval)
        assertEquals(500, config.syncLimit)
    }

    @Test
    fun `honors environment overrides and clamps sync settings`() {
        val config = RealtimeConfig.fromEnv(
            mapOf(
                "PORT" to "18081",
                "WISHPOOL_CORE_API_BASE_URL" to "http://core-api:8080/api",
                "WISHPOOL_REDIS_URI" to "redis://redis:6379",
                "WISHPOOL_REDIS_ENABLED" to "false",
                "WISHPOOL_REALTIME_POLL_INTERVAL_MS" to "100",
                "WISHPOOL_REALTIME_HEARTBEAT_INTERVAL_SECONDS" to "1",
                "WISHPOOL_REALTIME_SYNC_LIMIT" to "5000",
            ),
        )

        assertEquals(18081, config.port)
        assertEquals("http://core-api:8080/api/", config.coreApiBaseUrl.toString())
        assertEquals("redis://redis:6379", config.redisUri)
        assertFalse(config.redisEnabled)
        assertEquals(250.milliseconds, config.pollInterval)
        assertEquals(5.seconds, config.heartbeatInterval)
        assertEquals(1000, config.syncLimit)
    }
}
