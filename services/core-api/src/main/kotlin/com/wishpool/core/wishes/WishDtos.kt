package com.wishpool.core.wishes

import com.wishpool.core.media.MediaAssetResponse
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class CreateWishRequest(
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val title: String,
    val note: String? = null,
    val imageMediaId: UUID? = null,
    val requiredFragments: Int,
    val rewardMode: String,
    val fragmentVisualMode: String = "grid_reveal",
    val fragmentGridRows: Int? = null,
    val fragmentGridCols: Int? = null,
)

data class AttachWishImageRequest(
    val mediaId: UUID,
    val sourceType: String = "generated",
)

data class WishResponse(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val title: String,
    val note: String?,
    val imageMedia: MediaAssetResponse? = null,
    val requiredFragments: Int,
    val earnedFragments: Int,
    val rewardMode: String,
    val status: String,
    val fragmentVisual: WishFragmentVisualResponse,
)

data class WishFragmentVisualResponse(
    val mode: String,
    val rows: Int,
    val cols: Int,
    val revealed: Int,
    val total: Int,
    val mask: WishFragmentMaskResponse,
    val litIndexes: List<Int>,
)

data class WishFragmentMaskResponse(
    val version: Int,
    val mode: String,
    val rows: Int,
    val cols: Int,
    val total: Int,
    val revealOrder: List<Int>,
    val cells: List<WishFragmentCellResponse>,
)

data class WishFragmentCellResponse(
    val index: Int,
    val row: Int,
    val col: Int,
    val polygon: List<WishFragmentPointResponse>? = null,
)

data class WishFragmentPointResponse(
    val x: Double,
    val y: Double,
)

data class WishImageCandidateRequest(
    val familyId: UUID,
    val childId: UUID,
    val title: String,
    val note: String? = null,
    val limit: Int = 6,
)

data class WishImageCandidateResponse(
    val query: WishImageCandidateQuery,
    val items: List<WishImageCandidate>,
    val fallbackOptions: List<String> = listOf("upload", "skip"),
)

data class WishImageCandidateQuery(
    val normalizedTitle: String,
    val keywords: List<String>,
    val category: String?,
)

data class WishImageCandidate(
    val media: MediaAssetResponse,
    val score: Int,
    val reason: String,
    val sourceWishId: UUID? = null,
    val sourceWishTitle: String? = null,
    val lastUsedAt: OffsetDateTime,
)

data class CreateWishImageGenerationRequest(
    val familyId: UUID,
    val childId: UUID,
    val wishId: UUID? = null,
    val usageCode: String = "wish_card",
    val providerCode: String? = null,
    val title: String,
    val note: String? = null,
    val category: String? = null,
    val style: String = "warm_illustration",
    val aspectRatio: String = "1:1",
)

data class WishImageGenerationJobResponse(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val wishId: UUID?,
    val usageCode: String,
    val providerCode: String?,
    val title: String,
    val note: String?,
    val category: String?,
    val style: String,
    val aspectRatio: String,
    val status: String,
    val mediaAssetId: UUID?,
    val media: MediaAssetResponse? = null,
    val modelProvider: String?,
    val modelName: String?,
    val attemptCount: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class WishImageGenerationJobRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val wishId: UUID?,
    val usageCode: String,
    val providerCode: String?,
    val title: String,
    val note: String?,
    val category: String?,
    val style: String,
    val aspectRatio: String,
    val status: String,
    val mediaAssetId: UUID?,
    val modelProvider: String?,
    val modelName: String?,
    val prompt: String?,
    val attemptCount: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class RedeemWishRequest(
    val redeemedDate: LocalDate,
    val photoMediaIds: List<UUID>,
    val parentNote: String? = null,
    val childNote: String? = null,
)

data class WishRedemptionResponse(
    val id: UUID,
    val wishId: UUID,
    val redeemedDate: LocalDate,
    val parentNote: String?,
    val childNote: String?,
    val photos: List<MediaAssetResponse>,
    val createdAt: OffsetDateTime,
)

data class WishHistoryItemResponse(
    val wish: WishResponse,
    val redemption: WishRedemptionResponse? = null,
    val realizedCount: Int,
)

data class WishRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val title: String,
    val note: String?,
    val imageMediaId: UUID?,
    val requiredFragments: Int,
    val earnedFragments: Int,
    val rewardMode: String,
    val status: String,
    val fragmentVisualMode: String,
    val fragmentGridRows: Int?,
    val fragmentGridCols: Int?,
    val fragmentMaskJson: String?,
    val fragmentLitJson: String?,
)

data class WishRedemptionRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val wishId: UUID,
    val redeemedDate: LocalDate,
    val parentNote: String?,
    val childNote: String?,
    val createdAt: OffsetDateTime,
)
