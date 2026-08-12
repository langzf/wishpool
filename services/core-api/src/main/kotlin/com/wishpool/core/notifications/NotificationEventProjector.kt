package com.wishpool.core.notifications

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import java.util.UUID

@Component
class NotificationEventProjector(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
) {
    fun projectFamilyEvent(
        familyId: UUID,
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payloadJson: String,
    ) {
        val payload = objectMapper.readTree(payloadJson) as? ObjectNode ?: return
        when (eventType) {
            "submission.created" -> notifyParents(
                familyId = familyId,
                notificationType = "child_submission_created",
                title = "新的任务提交",
                body = "孩子完成了一项任务，等待家长审核。",
                relatedResourceType = aggregateType,
                relatedResourceId = aggregateId,
                dedupeSuffix = aggregateId.toString(),
            )
            "submission.ai_prechecked" -> notifyParents(
                familyId = familyId,
                notificationType = "ai_precheck_completed",
                title = "AI 预审已完成",
                body = payload["summary"]?.asString()?.take(120) ?: "AI 建议已准备好，家长可以快速查看。",
                relatedResourceType = aggregateType,
                relatedResourceId = aggregateId,
                dedupeSuffix = aggregateId.toString(),
            )
            "review.approved", "review.revision_requested" -> notifyChildDevices(
                familyId = familyId,
                childId = payload.uuid("childId") ?: return,
                notificationType = "review_completed",
                title = if (eventType == "review.approved") "任务通过啦" else "任务需要再改一下",
                body = if (eventType == "review.approved") "家长已经认可了这次完成。" else "家长留下了反馈，可以打开看看。",
                relatedResourceType = aggregateType,
                relatedResourceId = aggregateId,
                dedupeSuffix = aggregateId.toString(),
            )
            "reward.wish_fragment_granted" -> notifyChildDevices(
                familyId = familyId,
                childId = payload.uuid("childId") ?: return,
                notificationType = "wish_fragment_earned",
                title = "获得一块心愿碎片",
                body = "今天的核心任务完成度达标，心愿更近一步。",
                relatedResourceType = aggregateType,
                relatedResourceId = aggregateId,
                dedupeSuffix = "${payload.text("childId")}:${payload.text("date")}",
            )
            "wish.unlocked" -> {
                val childId = payload.uuid("childId") ?: return
                notifyParents(
                    familyId = familyId,
                    notificationType = "wish_unlocked",
                    title = "心愿已解锁",
                    body = "孩子的本周心愿可以兑现了。",
                    relatedResourceType = aggregateType,
                    relatedResourceId = aggregateId,
                    dedupeSuffix = aggregateId.toString(),
                )
                notifyChildDevices(
                    familyId = familyId,
                    childId = childId,
                    notificationType = "wish_unlocked",
                    title = "心愿已解锁",
                    body = "星光积累完成，可以和家长一起兑现心愿。",
                    relatedResourceType = aggregateType,
                    relatedResourceId = aggregateId,
                    dedupeSuffix = aggregateId.toString(),
                )
            }
            "memory.generated" -> notifyParents(
                familyId = familyId,
                notificationType = "wish_redeemed_memory_generated",
                title = "成长周卡已生成",
                body = "本周的任务、照片和心愿已经整理进纪念册。",
                relatedResourceType = aggregateType,
                relatedResourceId = aggregateId,
                dedupeSuffix = aggregateId.toString(),
            )
            "planning.weekly_plan_saved", "task.created", "task.postponed", "task.skipped" -> {
                payload.uuid("childId")?.let { childId ->
                    notifyChildDevices(
                        familyId = familyId,
                        childId = childId,
                        notificationType = "task_plan_changed",
                        title = "今日任务有更新",
                        body = "任务安排发生变化，可以刷新今日任务。",
                        relatedResourceType = aggregateType,
                        relatedResourceId = aggregateId,
                        dedupeSuffix = childId.toString(),
                    )
                }
            }
        }
    }

    private fun notifyParents(
        familyId: UUID,
        notificationType: String,
        title: String,
        body: String,
        relatedResourceType: String,
        relatedResourceId: UUID,
        dedupeSuffix: String,
    ) {
        val recipients = jdbcClient.sql(
            """
            select user_id
            from family_member
            where family_id = :family_id
              and role in ('parent_owner', 'parent')
              and status = 'active'
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .query(UUID::class.java)
            .list()
            .filterNotNull()
        recipients.forEach { userId ->
            createNotification(
                familyId = familyId,
                recipientUserId = userId,
                notificationType = notificationType,
                title = title,
                body = body,
                relatedResourceType = relatedResourceType,
                relatedResourceId = relatedResourceId,
                dedupeKey = "$notificationType:$userId:$dedupeSuffix",
            )
        }
    }

    private fun notifyChildDevices(
        familyId: UUID,
        childId: UUID,
        notificationType: String,
        title: String,
        body: String,
        relatedResourceType: String,
        relatedResourceId: UUID,
        dedupeSuffix: String,
    ) {
        val recipients = jdbcClient.sql(
            """
            select user_id
            from family_member
            where family_id = :family_id
              and child_id = :child_id
              and role = 'child_device'
              and status = 'active'
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .query(UUID::class.java)
            .list()
            .filterNotNull()
        recipients.forEach { userId ->
            createNotification(
                familyId = familyId,
                recipientUserId = userId,
                notificationType = notificationType,
                title = title,
                body = body,
                relatedResourceType = relatedResourceType,
                relatedResourceId = relatedResourceId,
                dedupeKey = "$notificationType:$userId:$dedupeSuffix",
            )
        }
    }

    private fun createNotification(
        familyId: UUID,
        recipientUserId: UUID,
        notificationType: String,
        title: String,
        body: String,
        relatedResourceType: String,
        relatedResourceId: UUID,
        dedupeKey: String,
    ) {
        jdbcClient.sql(
            """
            insert into notification_event (
              family_id, recipient_user_id, type, title, body,
              related_resource_type, related_resource_id, dedupe_key
            ) values (
              :family_id, :recipient_user_id, :type, :title, :body,
              :related_resource_type, :related_resource_id, :dedupe_key
            )
            on conflict (dedupe_key) where dedupe_key is not null
            do update set title = excluded.title,
                          body = excluded.body,
                          status = case
                            when notification_event.status = 'read' then notification_event.status
                            else 'pending'
                          end
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("recipient_user_id", recipientUserId)
            .param("type", notificationType)
            .param("title", title)
            .param("body", body)
            .param("related_resource_type", relatedResourceType)
            .param("related_resource_id", relatedResourceId)
            .param("dedupe_key", dedupeKey)
            .update()
    }

    private fun ObjectNode.uuid(name: String): UUID? =
        text(name)?.let(UUID::fromString)

    private fun ObjectNode.text(name: String): String? =
        get(name)?.takeIf { !it.isNull }?.asString()
}
