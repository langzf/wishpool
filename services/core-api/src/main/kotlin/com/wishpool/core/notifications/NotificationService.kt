package com.wishpool.core.notifications

import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ForbiddenError
import com.wishpool.core.shared.NotFoundError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import java.util.UUID

@Service
class NotificationService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val objectMapper: ObjectMapper,
) {
    fun listInbox(familyId: UUID, status: String?, limit: Int): NotificationListResponse {
        val user = currentUser.require()
        familyPolicy.requireMember(user, familyId)
        val effectiveLimit = limit.coerceIn(1, 100)
        val statusClause = if (status.isNullOrBlank()) {
            "and ne.status in ('pending', 'sent', 'failed', 'read')"
        } else {
            if (status !in PUBLIC_STATUSES) throw BadRequestError("Unsupported notification status.")
            "and ne.status = :status"
        }
        val query = jdbcClient.sql(
            """
            select id, family_id, recipient_user_id, recipient_device_id, type, title, body,
                   related_resource_type, related_resource_id, status, sent_at, read_at, created_at
            from notification_event ne
            where ne.family_id = :family_id
              and ne.recipient_user_id = :recipient_user_id
              $statusClause
            order by ne.created_at desc
            limit cast(:limit as integer)
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("recipient_user_id", user.userId)
            .param("limit", effectiveLimit)
        val rows = if (status.isNullOrBlank()) {
            query
        } else {
            query.param("status", status)
        }
            .query(::notificationEventRecord)
            .list()
        return NotificationListResponse(
            items = rows.map(::toResponse),
            nextCursor = rows.lastOrNull()?.createdAt,
        )
    }

    @Transactional
    fun markRead(request: MarkNotificationReadRequest): NotificationListResponse {
        val user = currentUser.require()
        if (request.notificationIds.isEmpty()) throw BadRequestError("notificationIds cannot be empty.")
        if (request.notificationIds.size > 100) throw BadRequestError("At most 100 notifications can be marked read.")
        val rows = jdbcClient.sql(
            """
            update notification_event
            set status = 'read',
                read_at = coalesce(read_at, now())
            where recipient_user_id = :recipient_user_id
              and id in (:ids)
              and status in ('pending', 'sent', 'failed')
            returning id, family_id, recipient_user_id, recipient_device_id, type, title, body,
                      related_resource_type, related_resource_id, status, sent_at, read_at, created_at
            """.trimIndent(),
        )
            .param("recipient_user_id", user.userId)
            .param("ids", request.notificationIds)
            .query(::notificationEventRecord)
            .list()
        return NotificationListResponse(rows.map(::toResponse), rows.lastOrNull()?.createdAt)
    }

    fun listPreferences(familyId: UUID): List<NotificationPreferenceResponse> {
        val user = currentUser.require()
        familyPolicy.requireMember(user, familyId)
        return jdbcClient.sql(
            """
            select id, family_id, user_id, notification_type, enabled, quiet_hours_json, channels_json, updated_at
            from notification_preference
            where family_id = :family_id
              and user_id = :user_id
            order by notification_type
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("user_id", user.userId)
            .query(notificationPreferenceRecord(objectMapper))
            .list()
            .map(::toPreferenceResponse)
    }

    @Transactional
    fun updatePreference(request: UpdateNotificationPreferenceRequest): NotificationPreferenceResponse {
        val user = currentUser.require()
        familyPolicy.requireMember(user, request.familyId)
        validateNotificationType(request.notificationType)
        val quietHours = request.quietHours ?: objectMapper.createObjectNode()
        val channels = request.channels ?: defaultChannels()
        val preference = jdbcClient.sql(
            """
            insert into notification_preference (
              family_id, user_id, notification_type, enabled, quiet_hours_json, channels_json
            ) values (
              :family_id, :user_id, :notification_type, :enabled,
              cast(:quiet_hours_json as jsonb), cast(:channels_json as jsonb)
            )
            on conflict (family_id, user_id, notification_type)
            do update set enabled = excluded.enabled,
                          quiet_hours_json = excluded.quiet_hours_json,
                          channels_json = excluded.channels_json,
                          updated_at = now()
            returning id, family_id, user_id, notification_type, enabled, quiet_hours_json, channels_json, updated_at
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("user_id", user.userId)
            .param("notification_type", request.notificationType)
            .param("enabled", request.enabled)
            .param("quiet_hours_json", objectMapper.writeValueAsString(quietHours))
            .param("channels_json", objectMapper.writeValueAsString(channels))
            .query(notificationPreferenceRecord(objectMapper))
            .single()
        audit(request.familyId, user.userId, "notification.preference_updated", "notification_preference", preference.id)
        return toPreferenceResponse(preference)
    }

    @Transactional
    fun registerPushToken(request: RegisterPushTokenRequest): NotificationDeviceResponse {
        val user = currentUser.require()
        familyPolicy.requireMember(user, request.familyId)
        if (request.platform !in PLATFORMS) throw BadRequestError("Unsupported device platform.")
        if (request.pushProvider !in PUSH_PROVIDERS) throw BadRequestError("Unsupported push provider.")
        if (request.pushToken.isBlank()) throw BadRequestError("pushToken cannot be blank.")
        val deviceId = user.deviceId ?: UUID.randomUUID()
        val device = jdbcClient.sql(
            """
            insert into device (
              id, user_id, family_id, platform, device_name, push_provider, push_token, last_seen_at
            ) values (
              :id, :user_id, :family_id, :platform, :device_name, :push_provider, :push_token, now()
            )
            on conflict (id)
            do update set family_id = excluded.family_id,
                          platform = excluded.platform,
                          device_name = excluded.device_name,
                          push_provider = excluded.push_provider,
                          push_token = excluded.push_token,
                          last_seen_at = now()
            returning id, platform, device_name, push_provider, push_token
            """.trimIndent(),
        )
            .param("id", deviceId)
            .param("user_id", user.userId)
            .param("family_id", request.familyId)
            .param("platform", request.platform)
            .param("device_name", request.deviceName)
            .param("push_provider", request.pushProvider)
            .param("push_token", request.pushToken)
            .query(::notificationDeviceRecord)
            .single()
        audit(request.familyId, user.userId, "notification.push_token_registered", "device", device.id)
        return toDeviceResponse(device)
    }

    @Transactional
    fun createNotification(request: CreateNotificationEventRequest): NotificationEventResponse {
        validateNotificationType(request.type)
        if (request.title.isBlank() || request.title.length > 120) throw BadRequestError("Notification title is invalid.")
        if (request.body.isBlank() || request.body.length > 1000) throw BadRequestError("Notification body is invalid.")
        val belongs = jdbcClient.sql(
            """
            select exists (
              select 1 from family_member
              where family_id = :family_id
                and user_id = :user_id
                and status = 'active'
            )
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("user_id", request.recipientUserId)
            .query(Boolean::class.java)
            .single()
        if (!belongs) throw ForbiddenError("Notification recipient is not an active family member.")

        val inserted = jdbcClient.sql(
            """
            insert into notification_event (
              family_id, recipient_user_id, recipient_device_id, type, title, body,
              related_resource_type, related_resource_id, dedupe_key
            ) values (
              :family_id, :recipient_user_id, :recipient_device_id, :type, :title, :body,
              :related_resource_type, :related_resource_id, :dedupe_key
            )
            on conflict (dedupe_key) where dedupe_key is not null
            do update set title = excluded.title,
                          body = excluded.body,
                          status = case
                            when notification_event.status = 'read' then notification_event.status
                            else 'pending'
                          end
            returning id, family_id, recipient_user_id, recipient_device_id, type, title, body,
                      related_resource_type, related_resource_id, status, sent_at, read_at, created_at
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("recipient_user_id", request.recipientUserId)
            .param("recipient_device_id", request.recipientDeviceId)
            .param("type", request.type)
            .param("title", request.title.trim())
            .param("body", request.body.trim())
            .param("related_resource_type", request.relatedResourceType)
            .param("related_resource_id", request.relatedResourceId)
            .param("dedupe_key", request.dedupeKey)
            .query(::notificationEventRecord)
            .single()
        return toResponse(inserted)
    }

    @Transactional
    fun claimPending(request: ClaimNotificationEventsRequest): ClaimNotificationEventsResponse {
        val limit = request.limit.coerceIn(1, 100)
        val rows = jdbcClient.sql(
            """
            with candidate as (
              select id
              from notification_event
              where status = 'pending'
              order by created_at
              limit cast(:limit as integer)
              for update skip locked
            )
            update notification_event ne
            set status = 'sent',
                sent_at = now()
            from candidate
            where ne.id = candidate.id
            returning ne.id, ne.family_id, ne.recipient_user_id, ne.recipient_device_id, ne.type, ne.title, ne.body,
                      ne.related_resource_type, ne.related_resource_id, ne.status, ne.sent_at, ne.read_at, ne.created_at
            """.trimIndent(),
        )
            .param("limit", limit)
            .query(::notificationEventRecord)
            .list()
        return ClaimNotificationEventsResponse(
            rows.map { notification ->
                NotificationDispatchItem(
                    notification = toResponse(notification),
                    recipientDevice = findDispatchDevice(notification)?.let(::toDeviceResponse),
                    preference = findPreference(notification)?.let(::toPreferenceResponse),
                )
            },
        )
    }

    @Transactional
    fun markDispatchResult(notificationId: UUID, request: NotificationDispatchResultRequest): NotificationEventResponse {
        if (request.status !in setOf("sent", "failed", "suppressed")) throw BadRequestError("Unsupported dispatch status.")
        val row = jdbcClient.sql(
            """
            update notification_event
            set status = :status,
                sent_at = case when :status = 'sent' then coalesce(sent_at, now()) else sent_at end
            where id = :id
            returning id, family_id, recipient_user_id, recipient_device_id, type, title, body,
                      related_resource_type, related_resource_id, status, sent_at, read_at, created_at
            """.trimIndent(),
        )
            .param("id", notificationId)
            .param("status", request.status)
            .query(::notificationEventRecord)
            .optional()
            .orElseThrow { NotFoundError("Notification event not found.") }
        return toResponse(row)
    }

    fun toResponse(record: NotificationEventRecord): NotificationEventResponse =
        NotificationEventResponse(
            id = record.id,
            familyId = record.familyId,
            recipientUserId = record.recipientUserId,
            recipientDeviceId = record.recipientDeviceId,
            type = record.type,
            title = record.title,
            body = record.body,
            relatedResourceType = record.relatedResourceType,
            relatedResourceId = record.relatedResourceId,
            status = record.status,
            sentAt = record.sentAt,
            readAt = record.readAt,
            createdAt = record.createdAt,
        )

    private fun toPreferenceResponse(record: NotificationPreferenceRecord): NotificationPreferenceResponse =
        NotificationPreferenceResponse(
            id = record.id,
            familyId = record.familyId,
            userId = record.userId,
            notificationType = record.notificationType,
            enabled = record.enabled,
            quietHours = record.quietHours,
            channels = record.channels,
            updatedAt = record.updatedAt,
        )

    private fun toDeviceResponse(record: NotificationDeviceRecord): NotificationDeviceResponse =
        NotificationDeviceResponse(
            id = record.id,
            platform = record.platform,
            deviceName = record.deviceName,
            pushProvider = record.pushProvider,
            pushToken = record.pushToken,
        )

    private fun findDispatchDevice(notification: NotificationEventRecord): NotificationDeviceRecord? {
        val deviceClause = if (notification.recipientDeviceId == null) "" else "and id = :device_id"
        val query = jdbcClient.sql(
            """
            select id, platform, device_name, push_provider, push_token
            from device
            where user_id = :user_id
              and family_id = :family_id
              and push_provider is not null
              and push_token is not null
              $deviceClause
            order by last_seen_at desc nulls last, created_at desc
            limit 1
            """.trimIndent(),
        )
            .param("user_id", notification.recipientUserId)
            .param("family_id", notification.familyId)
        return if (notification.recipientDeviceId == null) {
            query
        } else {
            query.param("device_id", notification.recipientDeviceId)
        }
            .query(::notificationDeviceRecord)
            .optional()
            .orElse(null)
    }

    private fun findPreference(notification: NotificationEventRecord): NotificationPreferenceRecord? =
        jdbcClient.sql(
            """
            select id, family_id, user_id, notification_type, enabled, quiet_hours_json, channels_json, updated_at
            from notification_preference
            where family_id = :family_id
              and user_id = :user_id
              and notification_type = :notification_type
            """.trimIndent(),
        )
            .param("family_id", notification.familyId)
            .param("user_id", notification.recipientUserId)
            .param("notification_type", notification.type)
            .query(notificationPreferenceRecord(objectMapper))
            .optional()
            .orElse(null)

    private fun defaultChannels(): ObjectNode =
        objectMapper.createObjectNode()
            .put("inbox", true)
            .put("push", true)

    private fun validateNotificationType(type: String) {
        if (type !in NOTIFICATION_TYPES) throw BadRequestError("Unsupported notification type.")
    }

    private fun audit(familyId: UUID, userId: UUID, action: String, resourceType: String, resourceId: UUID) {
        jdbcClient.sql(
            """
            insert into audit_log (
              family_id, actor_user_id, actor_role, action, resource_type, resource_id
            ) values (
              :family_id, :actor_user_id, 'member', :action, :resource_type, :resource_id
            )
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("actor_user_id", userId)
            .param("action", action)
            .param("resource_type", resourceType)
            .param("resource_id", resourceId)
            .update()
    }

    private companion object {
        val PUBLIC_STATUSES = setOf("pending", "sent", "failed", "read")
        val PLATFORMS = setOf("ios", "android", "ipad_os", "web", "admin_web")
        val PUSH_PROVIDERS = setOf("apns", "fcm", "huawei", "xiaomi", "oppo", "vivo")
        val NOTIFICATION_TYPES = setOf(
            "child_submission_created",
            "ai_precheck_completed",
            "review_completed",
            "wish_fragment_earned",
            "wish_unlocked",
            "wish_redeemed_memory_generated",
            "task_plan_changed",
        )
    }
}
