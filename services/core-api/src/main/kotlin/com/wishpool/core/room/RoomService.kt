package com.wishpool.core.room

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.idempotency.IdempotencyService
import com.wishpool.core.media.MediaService
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class RoomService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
    private val eventPublisher: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
    private val idempotencyService: IdempotencyService,
) {
    fun getRoomState(childId: UUID): RoomStateResponse {
        familyPolicy.requireCanAccessChild(currentUser.require(), childId)
        val theme = jdbcClient.sql("select room_theme from child_profile where id = :id")
            .param("id", childId)
            .query(String::class.java)
            .optional()
            .orElseThrow { NotFoundError("Child profile not found.") }
        val items = jdbcClient.sql(
            """
            select id, family_id, child_id, type, source_type, source_id, title,
                   media_asset_id, position_json::text, visible, unlocked_at
            from room_item
            where child_id = :child_id
              and visible = true
            order by unlocked_at, created_at
            """.trimIndent(),
        )
            .param("child_id", childId)
            .query(roomItemRecord(objectMapper))
            .list()
        return RoomStateResponse(childId = childId, theme = theme, items = items.map(::toResponse))
    }

    @Transactional
    fun arrangeRoomItem(itemId: UUID, request: ArrangeRoomItemRequest, idempotencyKey: String?): RoomItemResponse {
        if (request.position.isEmpty()) throw BadRequestError("position cannot be empty.")
        val existing = findItem(itemId) ?: throw NotFoundError("Room item not found.")
        val user = currentUser.require()
        familyPolicy.requireCanAccessChild(user, existing.childId)
        idempotencyService.find(existing.familyId, idempotencyKey, ROOM_ARRANGE_OPERATION)
            ?.let { return toResponse(findItem(it.resourceId) ?: throw NotFoundError("Room item not found.")) }
        val positionJson = objectMapper.writeValueAsString(request.position)
        val updated = jdbcClient.sql(
            """
            update room_item
            set position_json = cast(:position_json as jsonb)
            where id = :id
            returning id, family_id, child_id, type, source_type, source_id, title,
                      media_asset_id, position_json::text, visible, unlocked_at
            """.trimIndent(),
        )
            .param("id", itemId)
            .param("position_json", positionJson)
            .query(roomItemRecord(objectMapper))
            .single()
        eventPublisher.publishFamilyEvent(
            familyId = updated.familyId,
            eventType = "room.item_arranged",
            aggregateType = "room_item",
            aggregateId = updated.id,
            payload = mapOf(
                "roomItemId" to updated.id.toString(),
                "familyId" to updated.familyId.toString(),
                "childId" to updated.childId.toString(),
                "position" to request.position,
            ),
        )
        idempotencyService.remember(updated.familyId, idempotencyKey, ROOM_ARRANGE_OPERATION, "room_item", updated.id, user.userId)
        return toResponse(updated)
    }

    private fun findItem(itemId: UUID): RoomItemRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, type, source_type, source_id, title,
                   media_asset_id, position_json::text, visible, unlocked_at
            from room_item
            where id = :id
            """.trimIndent(),
        )
            .param("id", itemId)
            .query(roomItemRecord(objectMapper))
            .optional()
            .orElse(null)

    private fun toResponse(item: RoomItemRecord): RoomItemResponse =
        RoomItemResponse(
            id = item.id,
            childId = item.childId,
            type = item.type,
            title = item.title,
            media = item.mediaAssetId?.let { mediaService.findMedia(it)?.let(mediaService::toResponse) },
            position = item.position,
            visible = item.visible,
            unlockedAt = item.unlockedAt,
        )

    private companion object {
        const val ROOM_ARRANGE_OPERATION = "room.arrange"
    }
}
