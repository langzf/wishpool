package com.wishpool.core.notifications

import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun notificationEventRecord(rs: ResultSet, rowNum: Int): NotificationEventRecord =
    NotificationEventRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        recipientUserId = rs.getObject("recipient_user_id", UUID::class.java),
        recipientDeviceId = rs.getObject("recipient_device_id", UUID::class.java),
        type = rs.getString("type"),
        title = rs.getString("title"),
        body = rs.getString("body"),
        relatedResourceType = rs.getString("related_resource_type"),
        relatedResourceId = rs.getObject("related_resource_id", UUID::class.java),
        status = rs.getString("status"),
        sentAt = rs.getObject("sent_at", OffsetDateTime::class.java),
        readAt = rs.getObject("read_at", OffsetDateTime::class.java),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
    )

fun notificationPreferenceRecord(objectMapper: ObjectMapper): (ResultSet, Int) -> NotificationPreferenceRecord = { rs, _ ->
    NotificationPreferenceRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        notificationType = rs.getString("notification_type"),
        enabled = rs.getBoolean("enabled"),
        quietHours = objectMapper.readTree(rs.getString("quiet_hours_json")),
        channels = objectMapper.readTree(rs.getString("channels_json")),
        updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
    )
}

fun notificationDeviceRecord(rs: ResultSet, rowNum: Int): NotificationDeviceRecord =
    NotificationDeviceRecord(
        id = rs.getObject("id", UUID::class.java),
        platform = rs.getString("platform"),
        deviceName = rs.getString("device_name"),
        pushProvider = rs.getString("push_provider"),
        pushToken = rs.getString("push_token"),
    )
