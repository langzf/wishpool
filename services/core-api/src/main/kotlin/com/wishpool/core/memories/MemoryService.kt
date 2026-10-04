package com.wishpool.core.memories

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.idempotency.IdempotencyService
import com.wishpool.core.media.MediaService
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import com.wishpool.core.room.RoomItemResponse
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID

@Service
class MemoryService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
    private val eventPublisher: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
    private val idempotencyService: IdempotencyService,
) {
    fun listMemories(childId: UUID, cursor: String?, limit: Int?): MemoryTimelineResponse {
        val user = currentUser.require()
        familyPolicy.requireCanAccessChild(user, childId)
        val pageSize = limit?.coerceIn(1, 50) ?: 20
        val beforeStart = cursor?.let { java.time.LocalDate.parse(it) }
        val memories = jdbcClient.sql(
            """
            select id, family_id, child_id, week_id, start_date, end_date, title, summary_json::text, status
            from weekly_memory
            where child_id = :child_id
              and (cast(:before_start as date) is null or start_date < cast(:before_start as date))
            order by start_date desc
            limit cast(:limit as integer)
            """.trimIndent(),
        )
            .param("child_id", childId)
            .param("before_start", beforeStart)
            .param("limit", pageSize + 1)
            .query(weeklyMemoryRecord(objectMapper))
            .list()
        val visible = memories.take(pageSize)
        return MemoryTimelineResponse(
            items = visible.map(::toResponse),
            nextCursor = memories.getOrNull(pageSize)?.startDate?.toString(),
        )
    }

    fun getMemory(memoryId: UUID): WeeklyMemoryResponse {
        val memory = findMemory(memoryId) ?: throw NotFoundError("Memory not found.")
        familyPolicy.requireCanAccessChild(currentUser.require(), memory.childId)
        return toResponse(memory)
    }

    @Transactional
    fun exportMemory(memoryId: UUID, request: ExportMemoryRequest, idempotencyKey: String?): MemoryExportResponse {
        if (request.format !in setOf("pdf", "long_image")) throw BadRequestError("Unsupported memory export format.")
        val memory = findMemory(memoryId) ?: throw NotFoundError("Memory not found.")
        val user = currentUser.require()
        familyPolicy.requireParent(user, memory.familyId)
        idempotencyService.find(memory.familyId, idempotencyKey, MEMORY_EXPORT_OPERATION)
            ?.let { return getExport(it.resourceId) }
        val mediaId = UUID.randomUUID()
        val contentType = if (request.format == "pdf") "application/pdf" else "image/png"
        val storageKey = "families/${memory.familyId}/memory_export/$mediaId.${if (request.format == "pdf") "pdf" else "png"}"
        jdbcClient.sql(
            """
            insert into media_asset (
              id, family_id, child_id, purpose, storage_key, content_type, status,
              created_by, related_type, related_id
            ) values (
              :id, :family_id, :child_id, 'memory_export', :storage_key, :content_type,
              'upload_pending', :created_by, 'weekly_memory', :related_id
            )
            """.trimIndent(),
        )
            .param("id", mediaId)
            .param("family_id", memory.familyId)
            .param("child_id", memory.childId)
            .param("storage_key", storageKey)
            .param("content_type", contentType)
            .param("created_by", user.userId)
            .param("related_id", memory.id)
            .update()
        eventPublisher.publishFamilyEvent(
            familyId = memory.familyId,
            eventType = "memory.export_requested",
            aggregateType = "weekly_memory",
            aggregateId = memory.id,
            payload = mapOf(
                "memoryId" to memory.id.toString(),
                "familyId" to memory.familyId.toString(),
                "childId" to memory.childId.toString(),
                "format" to request.format,
                "mediaId" to mediaId.toString(),
            ),
        )
        val media = mediaService.findMedia(mediaId)?.let(mediaService::toResponse)
        idempotencyService.remember(memory.familyId, idempotencyKey, MEMORY_EXPORT_OPERATION, "media_asset", mediaId, user.userId)
        return MemoryExportResponse(id = mediaId, status = "requested", media = media)
    }

    private fun getExport(mediaId: UUID): MemoryExportResponse {
        val media = mediaService.findMedia(mediaId) ?: throw NotFoundError("Memory export not found.")
        return MemoryExportResponse(id = media.id, status = media.status, media = mediaService.toResponse(media))
    }

    @Transactional
    fun generateMemoryFromWorkflow(triggeredByEventId: UUID): WeeklyMemoryResponse {
        val payload = outboxPayload(triggeredByEventId)
        val wishId = payload.path("wishId").asString().takeIf { it.isNotBlank() }?.let(UUID::fromString)
            ?: throw BadRequestError("wishId is required for memory generation.")
        val wish = findWishForMemory(wishId) ?: throw NotFoundError("Wish not found for memory generation.")
        val plan = findPlanForWish(wish.id)
        val redeemedDate = payload.path("redeemedDate").asString().takeIf { it.isNotBlank() }?.let(LocalDate::parse)
            ?: LocalDate.now()
        val startDate = plan?.startDate ?: redeemedDate.minusDays((redeemedDate.dayOfWeek.value - 1).toLong())
        val endDate = plan?.endDate ?: startDate.plusDays(6)
        val childName = childNickname(wish.childId)
        // Build the complete replacement set before mutating memory_item. The plan id
        // is part of the scope: dates alone also match unrelated/adhoc tasks.
        val approvedTasks = approvedTasks(wish.familyId, wish.childId, startDate, endDate)
        val mediaItems = approvedSubmissionMedia(wish.childId, startDate, endDate)
        val title = "$childName 的${wish.weekId}成长纪念册"
        val summaryJson = objectMapper.writeValueAsString(
            mapOf(
                "summary" to "$childName 完成了 ${approvedTasks.size} 个被家长确认的任务，并兑现了心愿「${wish.title}」。",
                "wishTitle" to wish.title,
                "approvedTaskCount" to approvedTasks.size,
                "startDate" to startDate.toString(),
                "endDate" to endDate.toString(),
            ),
        )
        val memory = jdbcClient.sql(
            """
            insert into weekly_memory (
              family_id, child_id, week_id, start_date, end_date, wish_id, title,
              summary_json, status, generated_at
            ) values (
              :family_id, :child_id, :week_id, :start_date, :end_date, :wish_id, :title,
              cast(:summary_json as jsonb), 'generated', now()
            )
            on conflict (family_id, child_id, week_id) do update
            set wish_id = excluded.wish_id,
                start_date = excluded.start_date,
                end_date = excluded.end_date,
                title = excluded.title,
                summary_json = excluded.summary_json,
                status = 'generated',
                generated_at = now(),
                updated_at = now()
            returning id, family_id, child_id, week_id, start_date, end_date, title, summary_json::text, status
            """.trimIndent(),
        )
            .param("family_id", wish.familyId)
            .param("child_id", wish.childId)
            .param("week_id", wish.weekId)
            .param("start_date", startDate)
            .param("end_date", endDate)
            .param("wish_id", wish.id)
            .param("title", title)
            .param("summary_json", summaryJson)
            .query(weeklyMemoryRecord(objectMapper))
            .single()

        jdbcClient.sql("delete from memory_item where weekly_memory_id = :memory_id")
            .param("memory_id", memory.id)
            .update()
        approvedTasks.forEachIndexed { index, task ->
            jdbcClient.sql(
                """
                insert into memory_item (
                  weekly_memory_id, item_type, source_type, source_id, content_json, sort_order
                ) values (
                  :weekly_memory_id, 'approved_task', 'task_instance', :source_id,
                  cast(:content_json as jsonb), :sort_order
                )
                """.trimIndent(),
            )
                .param("weekly_memory_id", memory.id)
                .param("source_id", task.id)
                .param(
                    "content_json",
                    objectMapper.writeValueAsString(
                        mapOf(
                            "title" to task.title,
                            "category" to task.category,
                            "scheduledDate" to task.scheduledDate.toString(),
                        ),
                    ),
                )
                .param("sort_order", index)
                .update()
        }
        mediaItems.forEachIndexed { offset, item ->
            jdbcClient.sql(
                """
                insert into memory_item (
                  weekly_memory_id, item_type, source_type, source_id, media_asset_id, content_json, sort_order
                ) values (
                  :weekly_memory_id, :item_type, 'submission', :source_id, :media_asset_id,
                  cast(:content_json as jsonb), :sort_order
                )
                """.trimIndent(),
            )
                .param("weekly_memory_id", memory.id)
                .param("item_type", item.itemType)
                .param("source_id", item.submissionId)
                .param("media_asset_id", item.mediaAssetId)
                .param("content_json", objectMapper.writeValueAsString(item.content))
                .param("sort_order", approvedTasks.size + offset)
                .update()
        }
        unlockMemoryRoomItem(memory)
        eventPublisher.publishFamilyEvent(
            familyId = memory.familyId,
            eventType = "memory.generated",
            aggregateType = "weekly_memory",
            aggregateId = memory.id,
            payload = mapOf(
                "memoryId" to memory.id.toString(),
                "familyId" to memory.familyId.toString(),
                "childId" to memory.childId.toString(),
                "weekId" to memory.weekId,
                "title" to memory.title,
            ),
        )
        return toResponse(memory)
    }

    fun toResponse(memory: WeeklyMemoryRecord): WeeklyMemoryResponse =
        WeeklyMemoryResponse(
            id = memory.id,
            childId = memory.childId,
            weekId = memory.weekId,
            title = memory.title,
            summary = memory.summary,
            items = listMemoryItems(memory.id),
            status = memory.status,
        )

    @Transactional
    fun featureMemory(memoryId: UUID): RoomItemResponse {
        val memory = findMemory(memoryId) ?: throw NotFoundError("Memory not found.")
        familyPolicy.requireCanAccessChild(currentUser.require(), memory.childId)
        val existing = jdbcClient.sql(
            """
            select id, family_id, child_id, type, source_type, source_id, title,
                   media_asset_id, position_json::text, visible, unlocked_at
            from room_item
            where child_id = :child_id and type = 'featured_memory'
              and source_type = 'weekly_memory' and source_id = :source_id
            """.trimIndent(),
        ).param("child_id", memory.childId).param("source_id", memory.id)
            .query { rs, _ ->
                RoomItemResponse(
                    id = rs.getObject("id", UUID::class.java), childId = memory.childId,
                    type = rs.getString("type"), title = rs.getString("title"),
                    position = objectMapper.readTree(rs.getString("position_json")),
                    visible = rs.getBoolean("visible"),
                    unlockedAt = rs.getObject("unlocked_at", java.time.OffsetDateTime::class.java),
                )
            }.optional().orElse(null)
        if (existing != null) return existing
        val row = jdbcClient.sql(
            """
            insert into room_item (family_id, child_id, type, source_type, source_id, title, position_json, visible)
            values (:family_id, :child_id, 'featured_memory', 'weekly_memory', :source_id, :title,
                    cast(:position_json as jsonb), true)
            on conflict (child_id, type, source_type, source_id) where source_id is not null do update
              set title = excluded.title
            returning id, child_id, type, title, position_json::text, visible, unlocked_at
            """.trimIndent(),
        ).param("family_id", memory.familyId).param("child_id", memory.childId)
            .param("source_id", memory.id).param("title", memory.title)
            .param("position_json", """{"x":4,"y":2,"layer":1}""")
            .query { rs, _ ->
                RoomItemResponse(
                    id = rs.getObject("id", UUID::class.java), childId = memory.childId,
                    type = rs.getString("type"), title = rs.getString("title"),
                    position = objectMapper.readTree(rs.getString("position_json")),
                    visible = rs.getBoolean("visible"),
                    unlockedAt = rs.getObject("unlocked_at", java.time.OffsetDateTime::class.java),
                )
            }.single()
        return row
    }

    private fun findMemory(memoryId: UUID): WeeklyMemoryRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, week_id, start_date, end_date, title, summary_json::text, status
            from weekly_memory
            where id = :id
            """.trimIndent(),
        )
            .param("id", memoryId)
            .query(weeklyMemoryRecord(objectMapper))
            .optional()
            .orElse(null)

    private fun listMemoryItems(memoryId: UUID): List<JsonNode> =
        jdbcClient.sql(
            """
            select mi.id, mi.item_type, mi.source_type, mi.source_id, mi.media_asset_id,
                   mi.content_json::text,
                   ma.content_type, ma.storage_key, ma.status, ma.duration_sec,
                   md.storage_key as thumbnail_key
            from memory_item mi
            left join media_asset ma on ma.id = mi.media_asset_id
            left join lateral (
              select storage_key from media_derivative
              where media_asset_id = mi.media_asset_id and kind = 'thumbnail'
              order by created_at desc limit 1
            ) md on true
            where mi.weekly_memory_id = :weekly_memory_id
            order by mi.sort_order, mi.created_at
            """.trimIndent(),
        )
            .param("weekly_memory_id", memoryId)
            .query { rs, _ ->
                val node = (objectMapper.readTree(rs.getString("content_json"))
                    as? tools.jackson.databind.node.ObjectNode
                    ?: objectMapper.createObjectNode())
                node.put("id", rs.getObject("id", UUID::class.java).toString())
                node.put("itemType", rs.getString("item_type"))
                node.put("sourceType", rs.getString("source_type"))
                node.put("sourceId", rs.getObject("source_id", UUID::class.java).toString())

                val mediaAssetId = rs.getObject("media_asset_id", UUID::class.java)
                if (mediaAssetId != null) {
                    node.put("mediaAssetId", mediaAssetId.toString())
                    node.put("contentType", rs.getString("content_type"))
                    node.put(
                        "downloadUrl",
                        if (rs.getString("status") in setOf("uploaded", "processing", "ready", "failed")) {
                            mediaService.createDownloadUrl(rs.getString("storage_key"))
                        } else null,
                    )
                    node.put(
                        "thumbnailUrl",
                        rs.getString("thumbnail_key")?.let(mediaService::createDownloadUrl),
                    )
                    val duration = rs.getObject("duration_sec") as? Number
                    if (duration != null) node.put("durationSec", duration.toInt()) else node.putNull("durationSec")
                }
                node
            }
            .list()

    private fun approvedSubmissionMedia(childId: UUID, startDate: LocalDate, endDate: LocalDate): List<ApprovedMediaMemoryItem> =
        jdbcClient.sql(
            """
            select s.id as submission_id, s.submission_type, s.submitted_at, ti.title as task_title,
                   sm.media_asset_id, ma.content_type, ma.storage_key, ma.status, ma.duration_sec,
                   md.storage_key as thumbnail_key
            from submission s
            join task_instance ti on ti.id = s.task_instance_id
            join submission_media sm on sm.submission_id = s.id
            join media_asset ma on ma.id = sm.media_asset_id
            left join lateral (
              select storage_key from media_derivative
              where media_asset_id = ma.id and kind = 'thumbnail'
              order by created_at desc limit 1
            ) md on true
            where s.child_id = :child_id and s.status = 'approved'
              and ti.scheduled_date between :start_date and :end_date
            order by ti.scheduled_date, s.submitted_at, sm.sort_order
            """.trimIndent(),
        ).param("child_id", childId).param("start_date", startDate).param("end_date", endDate)
            .query { rs, _ ->
                val type = rs.getString("submission_type")
                val contentType = rs.getString("content_type")
                ApprovedMediaMemoryItem(
                    submissionId = rs.getObject("submission_id", UUID::class.java),
                    mediaAssetId = rs.getObject("media_asset_id", UUID::class.java),
                    itemType = when { type in setOf("photo", "audio", "video") -> type else -> contentType.substringBefore('/') },
                    content = mapOf(
                        "mediaAssetId" to rs.getObject("media_asset_id", UUID::class.java).toString(),
                        "sourceType" to "submission", "sourceId" to rs.getObject("submission_id", UUID::class.java).toString(),
                        "downloadUrl" to if (rs.getString("status") in setOf("uploaded", "processing", "ready", "failed")) mediaService.createDownloadUrl(rs.getString("storage_key")) else null,
                        "contentType" to contentType,
                        "thumbnailUrl" to rs.getString("thumbnail_key")?.let(mediaService::createDownloadUrl),
                        "durationSec" to rs.getObject("duration_sec"), "taskTitle" to rs.getString("task_title"),
                        "submittedAt" to rs.getObject("submitted_at", java.time.OffsetDateTime::class.java).toString(),
                        "title" to rs.getString("task_title"),
                    ),
                )
            }.list()

    private fun outboxPayload(eventId: UUID): JsonNode =
        jdbcClient.sql("select payload_json::text from outbox_event where id = :id")
            .param("id", eventId)
            .query(String::class.java)
            .optional()
            .map(objectMapper::readTree)
            .orElseThrow { NotFoundError("Outbox event not found.") }

    private fun findWishForMemory(wishId: UUID): WishMemoryContext? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, week_id, title
            from wish
            where id = :id
            """.trimIndent(),
        )
            .param("id", wishId)
            .query { rs, _ ->
                WishMemoryContext(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    weekId = rs.getString("week_id"),
                    title = rs.getString("title"),
                )
            }
            .optional()
            .orElse(null)

    private fun findPlanForWish(wishId: UUID): PlanMemoryContext? =
        jdbcClient.sql(
            """
            select wp.id, wp.start_date, wp.end_date
            from weekly_plan wp
            join wish w on w.id = :wish_id
            where wp.wish_id = :wish_id
               or (wp.wish_id is null
                   and wp.family_id = w.family_id
                   and wp.child_id = w.child_id
                   and wp.week_id = w.week_id)
            order by wp.created_at desc
            limit 1
            """.trimIndent(),
        )
            .param("wish_id", wishId)
            .query { rs, _ ->
                PlanMemoryContext(
                    id = rs.getObject("id", UUID::class.java),
                    startDate = rs.getDate("start_date").toLocalDate(),
                    endDate = rs.getDate("end_date").toLocalDate(),
                )
            }
            .optional()
            .orElse(null)

    private fun childNickname(childId: UUID): String =
        jdbcClient.sql("select nickname from child_profile where id = :id")
            .param("id", childId)
            .query(String::class.java)
            .optional()
            .orElse("孩子")

    private fun approvedTasks(familyId: UUID, childId: UUID, startDate: LocalDate, endDate: LocalDate): List<ApprovedTaskMemoryItem> =
        jdbcClient.sql(
            """
            select id, title, category, scheduled_date
            from task_instance
            where family_id = :family_id
              and child_id = :child_id
              and scheduled_date between :start_date and :end_date
              and status = 'approved'
            order by scheduled_date, sort_order
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("start_date", startDate)
            .param("end_date", endDate)
            .query { rs, _ ->
                ApprovedTaskMemoryItem(
                    id = rs.getObject("id", UUID::class.java),
                    title = rs.getString("title"),
                    category = rs.getString("category"),
                    scheduledDate = rs.getDate("scheduled_date").toLocalDate(),
                )
            }
            .list()

    private fun unlockMemoryRoomItem(memory: WeeklyMemoryRecord) {
        val existing = jdbcClient.sql(
            """
            select id
            from room_item
            where child_id = :child_id
              and type = 'memory_shelf'
              and source_type = 'weekly_memory'
              and source_id = :source_id
            """.trimIndent(),
        )
            .param("child_id", memory.childId)
            .param("source_id", memory.id)
            .query(UUID::class.java)
            .optional()
            .orElse(null)
        if (existing != null) return
        val roomItemId = UUID.randomUUID()
        jdbcClient.sql(
            """
            insert into room_item (
              id, family_id, child_id, type, source_type, source_id, title, position_json, visible
            ) values (
              :id, :family_id, :child_id, 'memory_shelf', 'weekly_memory', :source_id,
              :title, cast(:position_json as jsonb), true
            )
            """.trimIndent(),
        )
            .param("id", roomItemId)
            .param("family_id", memory.familyId)
            .param("child_id", memory.childId)
            .param("source_id", memory.id)
            .param("title", "纪念册书架")
            .param("position_json", """{"x":76,"y":62,"layer":1}""")
            .update()
        eventPublisher.publishFamilyEvent(
            familyId = memory.familyId,
            eventType = "room.item_unlocked",
            aggregateType = "room_item",
            aggregateId = roomItemId,
            payload = mapOf(
                "roomItemId" to roomItemId.toString(),
                "familyId" to memory.familyId.toString(),
                "childId" to memory.childId.toString(),
                "sourceType" to "weekly_memory",
                "sourceId" to memory.id.toString(),
            ),
        )
    }

    private companion object {
        const val MEMORY_EXPORT_OPERATION = "memory.export"
    }
}

private data class WishMemoryContext(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val title: String,
)

private data class PlanMemoryContext(
    val id: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

private data class ApprovedTaskMemoryItem(
    val id: UUID,
    val title: String,
    val category: String,
    val scheduledDate: LocalDate,
)

private data class ApprovedMediaMemoryItem(
    val submissionId: UUID,
    val mediaAssetId: UUID,
    val itemType: String,
    val content: Map<String, Any?>,
)
