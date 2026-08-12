package com.wishpool.core.memories

import com.wishpool.core.media.MediaAssetResponse
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID

data class MemoryTimelineResponse(
    val items: List<WeeklyMemoryResponse>,
    val nextCursor: String? = null,
)

data class WeeklyMemoryResponse(
    val id: UUID,
    val childId: UUID,
    val weekId: String,
    val title: String,
    val summary: JsonNode,
    val items: List<JsonNode>,
    val status: String,
)

data class ExportMemoryRequest(
    val format: String,
)

data class MemoryExportResponse(
    val id: UUID,
    val status: String,
    val media: MediaAssetResponse? = null,
)

data class WeeklyMemoryRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val title: String,
    val summary: JsonNode,
    val status: String,
)
