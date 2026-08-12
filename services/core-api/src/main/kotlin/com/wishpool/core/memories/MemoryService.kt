package com.wishpool.core.memories

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.media.MediaService
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
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
              and (:before_start is null or start_date < :before_start)
            order by start_date desc
            limit :limit
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
    fun exportMemory(memoryId: UUID, request: ExportMemoryRequest): MemoryExportResponse {
        if (request.format !in setOf("pdf", "long_image")) throw BadRequestError("Unsupported memory export format.")
        val memory = findMemory(memoryId) ?: throw NotFoundError("Memory not found.")
        val user = currentUser.require()
        familyPolicy.requireParent(user, memory.familyId)
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
        return MemoryExportResponse(id = mediaId, status = "requested", media = media)
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
        val approvedTasks = approvedTasks(wish.familyId, wish.childId, startDate, endDate)
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
            set title = excluded.title,
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
            select content_json::text
            from memory_item
            where weekly_memory_id = :weekly_memory_id
            order by sort_order, created_at
            """.trimIndent(),
        )
            .param("weekly_memory_id", memoryId)
            .query { rs, _ -> objectMapper.readTree(rs.getString("content_json")) }
            .list()

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
            select start_date, end_date
            from weekly_plan
            where wish_id = :wish_id
            order by created_at desc
            limit 1
            """.trimIndent(),
        )
            .param("wish_id", wishId)
            .query { rs, _ ->
                PlanMemoryContext(
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
}

private data class WishMemoryContext(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val title: String,
)

private data class PlanMemoryContext(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

private data class ApprovedTaskMemoryItem(
    val id: UUID,
    val title: String,
    val category: String,
    val scheduledDate: LocalDate,
)
