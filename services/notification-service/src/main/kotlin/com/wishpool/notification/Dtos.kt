package com.wishpool.notification

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class NotificationEventResponse(
    val id: UUID,
    val familyId: UUID,
    val recipientUserId: UUID,
    val recipientDeviceId: UUID?,
    val type: String,
    val title: String,
    val body: String,
    val relatedResourceType: String?,
    val relatedResourceId: UUID?,
    val status: String,
    val sentAt: OffsetDateTime?,
    val readAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
)

data class NotificationDeviceResponse(
    val id: UUID,
    val platform: String,
    val deviceName: String?,
    val pushProvider: String?,
    val pushToken: String?,
)

data class NotificationPreferenceResponse(
    val id: UUID,
    val familyId: UUID,
    val userId: UUID,
    val notificationType: String,
    val enabled: Boolean,
    val quietHours: JsonNode,
    val channels: JsonNode,
    val updatedAt: OffsetDateTime,
)

data class NotificationDispatchItem(
    val notification: NotificationEventResponse,
    val recipientDevice: NotificationDeviceResponse?,
    val preference: NotificationPreferenceResponse?,
)

data class ClaimNotificationEventsRequest(
    val limit: Int,
)

data class ClaimNotificationEventsResponse(
    val items: List<NotificationDispatchItem>,
)

data class NotificationDispatchResultRequest(
    val status: String,
    val providerMessageId: String? = null,
    val errorMessage: String? = null,
)

data class NotificationHealthResponse(
    val service: String = "notification-service",
    val status: String,
    val dispatchEnabled: Boolean,
)
