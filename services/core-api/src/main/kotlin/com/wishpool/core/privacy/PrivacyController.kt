package com.wishpool.core.privacy

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class PrivacyController(
    private val privacyService: PrivacyService,
) {
    @PostMapping("/privacy/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestDataExport(@Valid @RequestBody request: PrivacyRequestCreate): PrivacyRequestResponse =
        privacyService.createPrivacyRequest(request.copy(requestType = "export"))

    @PostMapping("/privacy/delete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestFamilyDeletion(@Valid @RequestBody request: PrivacyRequestCreate): PrivacyRequestResponse =
        privacyService.createPrivacyRequest(request.copy(requestType = "delete"))
}
