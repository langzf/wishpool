package com.wishpool.core.child

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ChildController(
    private val childService: ChildService,
) {
    @GetMapping("/families/{familyId}/children")
    fun listChildren(@PathVariable familyId: UUID): List<ChildProfileResponse> =
        childService.listChildren(familyId)

    @PostMapping("/families/{familyId}/children")
    @ResponseStatus(HttpStatus.CREATED)
    fun createChild(
        @PathVariable familyId: UUID,
        @Valid @RequestBody request: CreateChildRequest,
    ): ChildProfileResponse =
        childService.createChild(familyId, request)

    @PatchMapping("/children/{childId}")
    fun updateChild(
        @PathVariable childId: UUID,
        @Valid @RequestBody request: UpdateChildRequest,
    ): ChildProfileResponse =
        childService.updateChild(childId, request)
}
