package com.wishpool.core.http

import jakarta.servlet.http.HttpServletRequest
import com.wishpool.core.shared.ApiError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.net.URI

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ApiError::class)
    fun handleApiError(
        ex: ApiError,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = problem(ex.status, ex.status.reasonPhrase, request)
        problem.detail = ex.message
        return ResponseEntity.status(problem.status).body(problem)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        return validationProblem(ex.bindingResult.allErrors, request)
    }

    @ExceptionHandler(BindException::class)
    fun handleBindingValidation(
        ex: BindException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        return validationProblem(ex.bindingResult.allErrors, request)
    }

    private fun validationProblem(
        errors: List<org.springframework.validation.ObjectError>,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", request)
        problem.detail = "Request body or parameters failed validation."
        problem.setProperty(
            "errors",
            errors.map { error ->
                when (error) {
                    is FieldError -> mapOf(
                        "field" to error.field,
                        "message" to (error.defaultMessage ?: "Invalid value"),
                    )
                    else -> mapOf(
                        "object" to error.objectName,
                        "message" to (error.defaultMessage ?: "Invalid value"),
                    )
                }
            },
        )
        return ResponseEntity.status(problem.status).body(problem)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = problem(HttpStatus.BAD_REQUEST, "Malformed request body", request)
        problem.detail = ex.mostSpecificCause.message ?: "Request body could not be read."
        return ResponseEntity.status(problem.status).body(problem)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = problem(HttpStatus.BAD_REQUEST, "Bad request", request)
        problem.detail = ex.message
        return ResponseEntity.status(problem.status).body(problem)
    }

    @ExceptionHandler(ServletRequestBindingException::class)
    fun handleServletRequestBinding(
        ex: ServletRequestBindingException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = problem(HttpStatus.BAD_REQUEST, "Bad request", request)
        problem.detail = ex.message
        return ResponseEntity.status(problem.status).body(problem)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = problem(HttpStatus.BAD_REQUEST, "Bad request", request)
        problem.detail = ex.message
        return ResponseEntity.status(problem.status).body(problem)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        logger.error("Unhandled API exception", ex)
        val problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request)
        problem.detail = "Unexpected server error."
        problem.setProperty("exception", ex::class.simpleName ?: "Exception")
        return ResponseEntity.status(problem.status).body(problem)
    }

    private fun problem(
        status: HttpStatus,
        title: String,
        request: HttpServletRequest,
    ): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(status, title)
        problem.title = title
        problem.type = URI.create("https://wishpool.local/problems/${status.value()}")
        problem.instance = URI.create(request.requestURI)
        problem.setProperty("traceId", request.getAttribute(TraceIdFilter.MDC_TRACE_KEY) ?: request.getHeader(TraceIdFilter.TRACE_HEADER))
        return problem
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
