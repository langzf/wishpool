package com.wishpool.core.family

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class FamilyController(
    private val familyService: FamilyService,
) {
    @PostMapping("/families")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFamily(@Valid @RequestBody request: CreateFamilyRequest): FamilyResponse =
        familyService.createFamily(request)

    @GetMapping("/families/{familyId}")
    fun getFamily(@PathVariable familyId: UUID): FamilyResponse =
        familyService.getFamily(familyId)

    @GetMapping("/families/{familyId}/members")
    fun listMembers(@PathVariable familyId: UUID): List<FamilyMemberResponse> =
        familyService.listMembers(familyId)

    @PostMapping("/families/{familyId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    fun inviteParent(
        @PathVariable familyId: UUID,
        @Valid @RequestBody request: InviteParentRequest,
    ): FamilyInviteResponse =
        familyService.inviteParent(familyId, request)
}
