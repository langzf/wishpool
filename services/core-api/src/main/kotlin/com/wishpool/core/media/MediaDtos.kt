package com.wishpool.core.media

import java.time.OffsetDateTime
import java.util.UUID

data class CreateUploadSessionRequest(
    val familyId: UUID,
    val childId: UUID? = null,
    val purpose: String,
    val contentType: String,
    val sizeBytes: Long,
    val relatedResource: RelatedResource? = null,
)

data class RelatedResource(
    val type: String,
    val id: UUID,
)

data class UploadSessionResponse(
    val mediaId: UUID,
    val uploadUrl: String,
    val storageKey: String,
    val expiresAt: OffsetDateTime,
    val maxSizeBytes: Long,
)

data class FinalizeMediaRequest(
    val checksumSha256: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationSec: Int? = null,
)

data class MediaAssetResponse(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID?,
    val purpose: String,
    val storageKey: String,
    val contentType: String,
    val status: String,
    val downloadUrl: String? = null,
)

data class MediaProcessingSourceResponse(
    val media: MediaAssetResponse,
    val derivatives: List<MediaDerivativeResponse>,
    val processing: MediaProcessingPolicyResponse,
)

data class MediaProcessingPolicyResponse(
    val requiredKinds: List<String>,
    val sourceBucket: String,
    val attemptCount: Int,
)

data class MediaDerivativeResponse(
    val id: UUID,
    val mediaAssetId: UUID,
    val kind: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long?,
    val metadata: tools.jackson.databind.JsonNode,
    val createdAt: OffsetDateTime,
)

data class CompleteMediaProcessingRequest(
    val derivatives: List<MediaDerivativeInput>,
)

data class MediaDerivativeInput(
    val kind: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

data class FailMediaProcessingRequest(
    val errorCode: String,
    val errorMessage: String,
    val retryable: Boolean = true,
    val delaySeconds: Long? = null,
)

data class ClaimMediaProcessingRequest(
    val limit: Int = 10,
    val leaseSeconds: Long = 300,
)

data class ClaimMediaProcessingResponse(
    val items: List<MediaProcessingSourceResponse>,
)

data class MediaAssetRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID?,
    val purpose: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long?,
    val status: String,
    val relatedType: String?,
    val relatedId: UUID?,
)

data class MediaDerivativeRecord(
    val id: UUID,
    val mediaAssetId: UUID,
    val kind: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long?,
    val metadata: tools.jackson.databind.JsonNode,
    val createdAt: OffsetDateTime,
)
