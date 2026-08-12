package com.wishpool.core.http

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TraceIdFilterTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `health response carries trace id`() {
        mockMvc.get("/actuator/health") {
            header("X-Trace-Id", "trace-test")
        }.andExpect {
            status { isOk() }
            header { string("X-Trace-Id", "trace-test") }
        }
    }
}
