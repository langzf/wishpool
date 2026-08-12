package com.wishpool.core.media

import com.wishpool.core.internal.InternalAuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MediaController(
    private val service: MediaService,
    private val internalAuthService: InternalAuthService,
) {
    @PostMapping("/media/upload-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUploadSession(@Valid @RequestBody request: CreateUploadSessionRequest): UploadSessionResponse =
        service.createUploadSession(request)

    @PostMapping("/media/{mediaId}/finalize")
    fun finalizeMedia(
        @PathVariable mediaId: UUID,
        @Valid @RequestBody request: FinalizeMediaRequest,
    ): MediaAssetResponse =
        service.finalizeMedia(mediaId, request)

    @GetMapping("/internal/media/{mediaId}/processing-source")
    fun getProcessingSource(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable mediaId: UUID,
    ): MediaProcessingSourceResponse {
        internalAuthService.requireToken(internalToken)
        return service.getProcessingSource(mediaId)
    }

    @PostMapping("/internal/media/{mediaId}/processing-started")
    fun markProcessingStarted(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable mediaId: UUID,
    ): MediaAssetResponse {
        internalAuthService.requireToken(internalToken)
        return service.markProcessingStarted(mediaId)
    }

    @PostMapping("/internal/media/processing/claim")
    fun claimProcessingItems(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: ClaimMediaProcessingRequest,
    ): ClaimMediaProcessingResponse {
        internalAuthService.requireToken(internalToken)
        return service.claimProcessingItems(request)
    }

    @PostMapping("/internal/media/{mediaId}/processing-completed")
    fun completeProcessing(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable mediaId: UUID,
        @Valid @RequestBody request: CompleteMediaProcessingRequest,
    ): MediaAssetResponse {
        internalAuthService.requireToken(internalToken)
        return service.completeProcessing(mediaId, request)
    }

    @PostMapping("/internal/media/{mediaId}/processing-failed")
    fun failProcessing(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable mediaId: UUID,
        @Valid @RequestBody request: FailMediaProcessingRequest,
    ): MediaAssetResponse {
        internalAuthService.requireToken(internalToken)
        return service.failProcessing(mediaId, request)
    }
}
