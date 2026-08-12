package com.wishpool.core.notifications

import com.wishpool.core.internal.InternalAuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class NotificationController(
    private val service: NotificationService,
    private val internalAuthService: InternalAuthService,
) {
    @GetMapping("/notifications")
    fun listInbox(
        @RequestParam familyId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): NotificationListResponse =
        service.listInbox(familyId, status, limit)

    @PostMapping("/notifications/read")
    fun markRead(@Valid @RequestBody request: MarkNotificationReadRequest): NotificationListResponse =
        service.markRead(request)

    @GetMapping("/notification-preferences")
    fun listPreferences(@RequestParam familyId: UUID): List<NotificationPreferenceResponse> =
        service.listPreferences(familyId)

    @PutMapping("/notification-preferences")
    fun updatePreference(@Valid @RequestBody request: UpdateNotificationPreferenceRequest): NotificationPreferenceResponse =
        service.updatePreference(request)

    @PostMapping("/devices/push-token")
    fun registerPushToken(@Valid @RequestBody request: RegisterPushTokenRequest): NotificationDeviceResponse =
        service.registerPushToken(request)

    @PostMapping("/internal/notifications")
    fun createNotification(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: CreateNotificationEventRequest,
    ): NotificationEventResponse {
        internalAuthService.requireToken(internalToken)
        return service.createNotification(request)
    }

    @PostMapping("/internal/notifications/claim")
    fun claimPending(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: ClaimNotificationEventsRequest,
    ): ClaimNotificationEventsResponse {
        internalAuthService.requireToken(internalToken)
        return service.claimPending(request)
    }

    @PostMapping("/internal/notifications/{notificationId}/dispatch-result")
    fun markDispatchResult(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable notificationId: UUID,
        @Valid @RequestBody request: NotificationDispatchResultRequest,
    ): NotificationEventResponse {
        internalAuthService.requireToken(internalToken)
        return service.markDispatchResult(notificationId, request)
    }
}
