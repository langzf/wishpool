package com.wishpool.notification

import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import tools.jackson.module.kotlin.jacksonObjectMapper

class NotificationDispatcherTests {
    @Test
    fun `disabled dispatcher never reports a notification as sent`() {
        val config = NotificationConfig(
            port = 8084,
            coreApiBaseUrl = URI.create("http://localhost:8080/"),
            internalToken = "test",
            pollInterval = kotlin.time.Duration.parse("1s"),
            claimLimit = 1,
            dispatchEnabled = false,
        )
        val client = RecordingCoreApiClient(config, dispatchItem())

        val dispatched = NotificationDispatcher(config, client).dispatchOnce()

        assertEquals(1, dispatched)
        assertEquals("failed", client.result?.status)
    }

    private fun dispatchItem(): NotificationDispatchItem {
        val now = OffsetDateTime.now()
        val mapper = jacksonObjectMapper()
        return NotificationDispatchItem(
            notification = NotificationEventResponse(
                id = UUID.randomUUID(), familyId = UUID.randomUUID(), recipientUserId = UUID.randomUUID(),
                recipientDeviceId = null, type = "review_completed", title = "title", body = "body",
                relatedResourceType = null, relatedResourceId = null, status = "pending", sentAt = null,
                readAt = null, createdAt = now,
            ),
            recipientDevice = null,
            preference = NotificationPreferenceResponse(
                id = UUID.randomUUID(), familyId = UUID.randomUUID(), userId = UUID.randomUUID(),
                notificationType = "review_completed", enabled = true,
                quietHours = mapper.createObjectNode(), channels = mapper.createObjectNode(), updatedAt = now,
            ),
        )
    }
}

private class RecordingCoreApiClient(
    config: NotificationConfig,
    private val item: NotificationDispatchItem,
) : CoreApiClient(config) {
    var result: NotificationDispatchResultRequest? = null

    override fun claimNotifications() = ClaimNotificationEventsResponse(listOf(item))

    override fun markDispatchResult(notificationId: UUID, request: NotificationDispatchResultRequest): NotificationEventResponse {
        result = request
        return item.notification
    }
}
