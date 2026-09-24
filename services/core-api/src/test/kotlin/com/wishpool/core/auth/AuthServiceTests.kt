package com.wishpool.core.auth

import com.wishpool.core.security.CurrentUser
import com.wishpool.core.security.TokenService
import com.wishpool.core.shared.Hashing
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class AuthServiceTests {
    private val clock = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC)
    private val hashing = Hashing()

    @Test
    fun `requestPhoneCode uses fixed code when local debug phone code is enabled`() {
        val jdbcClient = testJdbcClient()
        val service = authService(jdbcClient, localDebugPhoneCode = true)

        val created = service.requestPhoneCode(
            PhoneCodeRequest(
                phoneNumber = " 138-0013 8000 ",
                purpose = "login",
            ),
        )

        assertEquals("123456", created.debugCode)
        assertEquals(clock.instant().plusSeconds(300), created.expiresAt.toInstant())

        val row = jdbcClient.sql(
            """
            select phone_number, purpose, code_hash
            from phone_verification_code
            where verification_token_hash = :verification_token_hash
            """.trimIndent(),
        )
            .param("verification_token_hash", hashing.sha256(created.verificationToken))
            .query { rs, _ ->
                Triple(
                    rs.getString("phone_number"),
                    rs.getString("purpose"),
                    rs.getString("code_hash"),
                )
            }
            .single()

        assertEquals("13800138000", row.first)
        assertEquals("login", row.second)
        assertEquals(hashing.sha256("123456"), row.third)
    }

    private fun authService(jdbcClient: JdbcClient, localDebugPhoneCode: Boolean): AuthService =
        AuthService(
            jdbcClient = jdbcClient,
            hashing = hashing,
            tokenService = TokenService("test-secret", clock),
            currentUser = CurrentUser(),
            clock = clock,
            accessTokenTtlSec = 60,
            refreshTokenTtlSec = 600,
            phoneCodeTtlSec = 300,
            localDebugPhoneCode = localDebugPhoneCode,
        )

    private fun testJdbcClient(): JdbcClient {
        val dataSource = DriverManagerDataSource().apply {
            setDriverClassName("org.h2.Driver")
            url = "jdbc:h2:mem:${System.nanoTime()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
        val jdbcClient = JdbcClient.create(dataSource)
        jdbcClient.sql(
            """
            create table phone_verification_code (
              id uuid default random_uuid() primary key,
              phone_number varchar(32) not null,
              purpose varchar(32) not null,
              verification_token_hash varchar(64) not null,
              code_hash varchar(64) not null,
              expires_at timestamp with time zone not null,
              attempt_count int not null default 0,
              status varchar(32) not null default 'active',
              consumed_at timestamp with time zone
            )
            """.trimIndent(),
        ).update()
        return jdbcClient
    }
}
