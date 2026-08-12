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
