package com.wishpool.core.auth

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/auth/phone-codes")
    fun requestPhoneCode(@Valid @RequestBody request: PhoneCodeRequest): PhoneCodeCreated =
        authService.requestPhoneCode(request)

    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthTokenPair =
        authService.login(request)

    @PostMapping("/auth/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): AuthTokenPair =
        authService.refresh(request)

    @GetMapping("/me")
    fun me(): MeResponse = authService.me()
}
