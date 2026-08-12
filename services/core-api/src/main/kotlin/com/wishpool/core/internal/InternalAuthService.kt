package com.wishpool.core.internal

import com.wishpool.core.shared.UnauthorizedError
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class InternalAuthService(
    @Value("\${wishpool.internal.token}") private val internalToken: String,
) {
    fun requireToken(headerToken: String?) {
        if (headerToken == null || headerToken != internalToken) {
            throw UnauthorizedError("Internal token is required.")
        }
    }
}
