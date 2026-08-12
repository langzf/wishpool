package com.wishpool.core.system

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SystemController {
    @GetMapping("/internal/version")
    fun version(): VersionResponse =
        VersionResponse(
            service = "core-api",
            status = "ok",
        )
}

data class VersionResponse(
    val service: String,
    val status: String,
)
