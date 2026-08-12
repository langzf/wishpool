package com.wishpool.core.shared

import org.springframework.http.HttpStatus

open class ApiError(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)

class UnauthorizedError(message: String = "Authentication is required.") : ApiError(HttpStatus.UNAUTHORIZED, message)

class ForbiddenError(message: String = "The current user is not allowed to perform this action.") : ApiError(HttpStatus.FORBIDDEN, message)

class NotFoundError(message: String = "Resource not found.") : ApiError(HttpStatus.NOT_FOUND, message)

class ConflictError(message: String = "Resource state conflict.") : ApiError(HttpStatus.CONFLICT, message)

class BadRequestError(message: String = "Bad request.") : ApiError(HttpStatus.BAD_REQUEST, message)
