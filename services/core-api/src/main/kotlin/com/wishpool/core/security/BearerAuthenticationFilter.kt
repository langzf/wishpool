package com.wishpool.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class BearerAuthenticationFilter(
    private val tokenService: TokenService,
    private val currentUser: CurrentUser,
    private val jdbcClient: JdbcClient,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()

        if (token != null) {
            val claims = tokenService.parseAccessToken(token)
            if (sessionIsActive(claims.sessionId)) {
                currentUser.set(AuthenticatedUser(claims.userId, claims.sessionId, claims.deviceId))
            }
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            currentUser.clear()
        }
    }

    private fun sessionIsActive(sessionId: UUID): Boolean =
        jdbcClient.sql(
            """
            select count(*)
            from auth_session
            where id = :session_id
              and status = 'active'
              and expires_at > now()
            """.trimIndent(),
        )
            .param("session_id", sessionId)
            .query(Int::class.java)
            .single() == 1
}
