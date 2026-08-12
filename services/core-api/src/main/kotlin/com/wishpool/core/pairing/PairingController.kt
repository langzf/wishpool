package com.wishpool.core.pairing

import com.wishpool.core.auth.AuthTokenPair
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PairingController(
    private val pairingService: PairingService,
) {
    @PostMapping("/families/{familyId}/pairing-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPairingSession(
        @PathVariable familyId: UUID,
        @Valid @RequestBody request: CreatePairingSessionRequest,
    ): PairingSessionCreated =
        pairingService.createPairingSession(familyId, request)

    @PostMapping("/pairing/consume")
    fun consumePairingCode(@Valid @RequestBody request: ConsumePairingCodeRequest): AuthTokenPair =
        pairingService.consumePairingCode(request)
}
