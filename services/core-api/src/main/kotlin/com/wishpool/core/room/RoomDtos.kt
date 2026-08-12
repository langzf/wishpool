package com.wishpool.core.room

import com.wishpool.core.media.MediaAssetResponse
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class RoomStateResponse(
    val childId: UUID,
    val theme: String?,
    val items: List<RoomItemResponse>,
)

data class RoomItemResponse(
    val id: UUID,
    val childId: UUID,
    val type: String,
    val title: String,
    val media: MediaAssetResponse? = null,
    val position: JsonNode,
    val visible: Boolean,
    val unlockedAt: OffsetDateTime,
)

data class ArrangeRoomItemRequest(
    val position: Map<String, Any?>,
)

data class RoomItemRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val type: String,
    val sourceType: String,
    val sourceId: UUID?,
    val title: String,
    val mediaAssetId: UUID?,
    val position: JsonNode,
    val visible: Boolean,
    val unlockedAt: OffsetDateTime,
)
