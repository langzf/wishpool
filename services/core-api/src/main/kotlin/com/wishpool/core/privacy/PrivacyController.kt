package com.wishpool.core.privacy

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

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

    @GetMapping("/families/{familyId}/privacy-requests")
    fun listPrivacyRequests(@PathVariable familyId: UUID): PrivacyRequestListResponse = privacyService.listPrivacyRequests(familyId)
}
