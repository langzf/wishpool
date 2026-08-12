package com.wishpool.core.http

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        request.setAttribute(MDC_TRACE_KEY, traceId)
        response.setHeader(TRACE_HEADER, traceId)
        MDC.put(MDC_TRACE_KEY, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_TRACE_KEY)
        }
    }

    companion object {
        const val TRACE_HEADER = "X-Trace-Id"
        const val MDC_TRACE_KEY = "traceId"
    }
}
