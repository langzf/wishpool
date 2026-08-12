package com.wishpool.core.notifications

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

data class NotificationListResponse(
    val items: List<NotificationEventResponse>,
    val nextCursor: OffsetDateTime?,
)

data class MarkNotificationReadRequest(
    val notificationIds: List<UUID>,
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

data class UpdateNotificationPreferenceRequest(
    val familyId: UUID,
    val notificationType: String,
    val enabled: Boolean,
    val quietHours: JsonNode? = null,
    val channels: JsonNode? = null,
)

data class RegisterPushTokenRequest(
    val familyId: UUID,
    val platform: String,
    val pushProvider: String,
    val pushToken: String,
    val deviceName: String? = null,
)

data class ClaimNotificationEventsRequest(
    val limit: Int = 50,
)

data class NotificationDispatchItem(
    val notification: NotificationEventResponse,
    val recipientDevice: NotificationDeviceResponse?,
    val preference: NotificationPreferenceResponse?,
)

data class NotificationDeviceResponse(
    val id: UUID,
    val platform: String,
    val deviceName: String?,
    val pushProvider: String?,
    val pushToken: String?,
)

data class ClaimNotificationEventsResponse(
    val items: List<NotificationDispatchItem>,
)

data class NotificationDispatchResultRequest(
    val status: String,
    val providerMessageId: String? = null,
    val errorMessage: String? = null,
)

data class CreateNotificationEventRequest(
    val familyId: UUID,
    val recipientUserId: UUID,
    val recipientDeviceId: UUID? = null,
    val type: String,
    val title: String,
    val body: String,
    val relatedResourceType: String? = null,
    val relatedResourceId: UUID? = null,
    val dedupeKey: String? = null,
)

data class NotificationEventRecord(
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

data class NotificationPreferenceRecord(
    val id: UUID,
    val familyId: UUID,
    val userId: UUID,
    val notificationType: String,
    val enabled: Boolean,
    val quietHours: JsonNode,
    val channels: JsonNode,
    val updatedAt: OffsetDateTime,
)

data class NotificationDeviceRecord(
    val id: UUID,
    val platform: String,
    val deviceName: String?,
    val pushProvider: String?,
    val pushToken: String?,
)
