package com.wishpool.core.outbox

import com.wishpool.core.internal.InternalAuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class OutboxController(
    private val internalAuthService: InternalAuthService,
    private val outboxService: OutboxService,
) {
    @PostMapping("/internal/outbox/events/claim")
    fun claimEvents(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: ClaimOutboxEventsRequest,
    ): OutboxClaimResponse {
        internalAuthService.requireToken(internalToken)
        return outboxService.claim(request)
    }

    @PostMapping("/internal/outbox/events/{eventId}/published")
    fun markPublished(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable eventId: UUID,
    ): OutboxAckResponse {
        internalAuthService.requireToken(internalToken)
        return outboxService.markPublished(eventId)
    }

    @PostMapping("/internal/outbox/events/{eventId}/retry")
    fun scheduleRetry(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable eventId: UUID,
        @Valid @RequestBody request: RetryOutboxEventRequest,
    ): OutboxEventResponse {
        internalAuthService.requireToken(internalToken)
        return outboxService.scheduleRetry(eventId, request)
    }

    @PostMapping("/internal/outbox/archive")
    fun archive(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @RequestParam(defaultValue = "false") dryRun: Boolean,
        @RequestParam(defaultValue = "5000") batchSize: Int,
        @RequestParam(defaultValue = "20") maxBatches: Int,
    ): OutboxArchiveResponse {
        internalAuthService.requireToken(internalToken)
        return outboxService.archive(dryRun, batchSize, maxBatches)
    }
}
