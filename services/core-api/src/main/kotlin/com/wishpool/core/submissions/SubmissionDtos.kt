package com.wishpool.core.submissions

import com.wishpool.core.media.MediaAssetResponse
import com.wishpool.core.ai.AiPrecheckResponse
import com.wishpool.core.reviews.ReviewResponse
import com.wishpool.core.tasks.TaskInstanceResponse
import java.time.OffsetDateTime
import java.util.UUID

data class CreateSubmissionRequest(
    val taskInstanceId: UUID,
    val clientMutationId: String,
    val mediaAssetIds: List<UUID>,
    val submittedAtClient: OffsetDateTime? = null,
)

data class SubmissionResponse(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val taskInstanceId: UUID,
    val attemptNo: Int,
    val submissionType: String,
    val status: String,
    val submittedAt: OffsetDateTime,
)

data class SubmissionDetailResponse(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val taskInstanceId: UUID,
    val attemptNo: Int,
    val submissionType: String,
    val status: String,
    val submittedAt: OffsetDateTime,
    val task: TaskInstanceResponse,
    val media: List<MediaAssetResponse>,
    val aiPrecheck: AiPrecheckResponse? = null,
    val review: ReviewResponse? = null,
)

data class SubmissionRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val taskInstanceId: UUID,
    val attemptNo: Int,
    val submissionType: String,
    val status: String,
    val submittedAt: OffsetDateTime,
) {
    fun toResponse(): SubmissionResponse =
        SubmissionResponse(
            id = id,
            familyId = familyId,
            childId = childId,
            taskInstanceId = taskInstanceId,
            attemptNo = attemptNo,
            submissionType = submissionType,
            status = status,
            submittedAt = submittedAt,
        )
}
