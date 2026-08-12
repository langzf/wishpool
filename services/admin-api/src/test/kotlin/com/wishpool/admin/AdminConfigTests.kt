package com.wishpool.admin

import kotlin.test.Test
import kotlin.test.assertEquals

class AdminConfigTests {
    @Test
    fun `loads local defaults`() {
        val config = AdminConfig.fromEnv(emptyMap())

        assertEquals(8083, config.port)
        assertEquals("http://localhost:8080/", config.coreApiBaseUrl.toString())
        assertEquals("wishpool-local-internal-token", config.internalToken)
        assertEquals("wishpool-local-admin-token", config.adminToken)
        assertEquals(5000, config.requestTimeout.inWholeMilliseconds)
    }

    @Test
    fun `honors overrides and clamps request timeout`() {
        val config = AdminConfig.fromEnv(
            mapOf(
                "PORT" to "18083",
                "WISHPOOL_CORE_API_BASE_URL" to "http://core-api:8080/api",
                "WISHPOOL_INTERNAL_TOKEN" to "secret",
                "WISHPOOL_ADMIN_TOKEN" to "admin-secret",
                "WISHPOOL_ADMIN_REQUEST_TIMEOUT_MS" to "100",
            ),
        )

        assertEquals(18083, config.port)
        assertEquals("http://core-api:8080/api/", config.coreApiBaseUrl.toString())
        assertEquals("secret", config.internalToken)
        assertEquals("admin-secret", config.adminToken)
        assertEquals(500, config.requestTimeout.inWholeMilliseconds)
    }
}
