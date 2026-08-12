package com.wishpool.core.security

import com.wishpool.core.shared.UnauthorizedError
import org.springframework.stereotype.Component
import java.util.UUID

data class AuthenticatedUser(
    val userId: UUID,
    val sessionId: UUID,
    val deviceId: UUID?,
)

@Component
class CurrentUser {
    private val holder = ThreadLocal<AuthenticatedUser?>()

    fun set(user: AuthenticatedUser?) {
        holder.set(user)
    }

    fun clear() {
        holder.remove()
    }

    fun getOrNull(): AuthenticatedUser? = holder.get()

    fun require(): AuthenticatedUser = holder.get() ?: throw UnauthorizedError()
}
