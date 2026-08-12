package com.wishpool.core.privacy

import com.wishpool.core.media.MediaAssetResponse
import java.time.OffsetDateTime
import java.util.UUID

data class PrivacyRequestCreate(
    val familyId: UUID,
    val requestType: String,
    val confirmationText: String,
    val reason: String? = null,
)

data class PrivacyRequestResponse(
    val id: UUID,
    val familyId: UUID,
    val requestType: String,
    val status: String,
    val exportMedia: MediaAssetResponse? = null,
    val createdAt: OffsetDateTime,
)

data class PrivacyRequestRecord(
    val id: UUID,
    val familyId: UUID,
    val requestType: String,
    val status: String,
    val requestedBy: UUID,
    val exportMediaId: UUID?,
    val reason: String?,
    val createdAt: OffsetDateTime,
)
