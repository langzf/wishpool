package com.wishpool.core.http

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiExceptionHandlerTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `malformed json request body returns bad request problem`() {
        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"provider":"phone"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.title") { value("Malformed request body") }
            jsonPath("$.status") { value(400) }
            jsonPath("$.traceId") { exists() }
        }
    }
}
