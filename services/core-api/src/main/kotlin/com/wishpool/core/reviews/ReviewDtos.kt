package com.wishpool.core.reviews

import com.wishpool.core.media.MediaAssetResponse
import com.wishpool.core.submissions.SubmissionResponse
import com.wishpool.core.tasks.TaskInstanceResponse
import java.time.OffsetDateTime
import java.util.UUID

data class PendingReviewCardResponse(
    val submission: SubmissionResponse,
    val task: TaskInstanceResponse,
    val aiSummary: String? = null,
    val thumbnailMedia: MediaAssetResponse? = null,
)

data class ReviewSubmissionRequest(
    val submissionId: UUID,
    val decision: String,
    val feedback: FeedbackInput? = null,
)

data class FeedbackInput(
    val emoji: String? = null,
    val text: String? = null,
    val audioMediaId: UUID? = null,
)

data class ReviewResponse(
    val id: UUID,
    val submissionId: UUID,
    val taskInstanceId: UUID,
    val decision: String,
    val reviewedBy: UUID,
    val feedback: FeedbackResponse? = null,
    val createdAt: OffsetDateTime,
)

data class FeedbackResponse(
    val emoji: String? = null,
    val text: String? = null,
    val audioMedia: MediaAssetResponse? = null,
)

data class RevokeReviewRequest(
    val reason: String,
)

data class ReviewRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val submissionId: UUID,
    val taskInstanceId: UUID,
    val decision: String,
    val reviewedBy: UUID,
    val createdAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
    val feedbackEmoji: String?,
    val feedbackText: String?,
    val audioMediaId: UUID?,
)
