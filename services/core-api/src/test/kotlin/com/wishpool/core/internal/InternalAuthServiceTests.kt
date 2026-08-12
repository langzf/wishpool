package com.wishpool.core.internal

import com.wishpool.core.shared.UnauthorizedError
import kotlin.test.Test
import kotlin.test.assertFailsWith

class InternalAuthServiceTests {
    private val service = InternalAuthService("secret-token")

    @Test
    fun `matching token is accepted`() {
        service.requireToken("secret-token")
    }

    @Test
    fun `missing or mismatched token is rejected`() {
        assertFailsWith<UnauthorizedError> {
            service.requireToken(null)
        }
        assertFailsWith<UnauthorizedError> {
            service.requireToken("wrong-token")
        }
    }
}
