package com.wishpool.core.http

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class RequestTraceAttributes {
    fun traceId(request: HttpServletRequest): String? =
        request.getHeader(TraceIdFilter.TRACE_HEADER)
}
