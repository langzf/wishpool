package com.wishpool.core.submissions

import com.wishpool.core.ai.AiPrecheckService
import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.media.MediaAssetRecord
import com.wishpool.core.media.MediaService
import com.wishpool.core.media.mediaAssetRecord
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.ForbiddenError
import com.wishpool.core.shared.NotFoundError
import com.wishpool.core.reviews.FeedbackResponse
import com.wishpool.core.reviews.ReviewRecord
import com.wishpool.core.reviews.ReviewResponse
import com.wishpool.core.reviews.reviewRecord
import com.wishpool.core.tasks.TaskInstanceResponse
import com.wishpool.core.tasks.taskInstanceResponse
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class SubmissionService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
    private val aiPrecheckService: AiPrecheckService,
    private val eventPublisher: DomainEventPublisher,
    private val clock: Clock,
) {
    @Transactional
    fun createSubmission(request: CreateSubmissionRequest): SubmissionResponse {
        val user = currentUser.require()
        if (request.clientMutationId.isBlank()) throw BadRequestError("clientMutationId cannot be blank.")
        if (user.deviceId == null) throw BadRequestError("Submitting a task requires a registered device.")

        val task = findTaskForSubmission(request.taskInstanceId) ?: throw NotFoundError("Task not found.")
        familyPolicy.requireCanAccessChild(user, task.childId)
        val existing = findSubmissionByMutationId(task.familyId, request.clientMutationId)
        if (existing != null) return existing.toResponse()

        if (task.status !in setOf("todo", "needs_revision", "submitted")) {
            throw ConflictError("Task is not open for submission.")
        }
        val media = loadAndValidateMedia(request.mediaAssetIds, task)
        val submittedAt = request.submittedAtClient ?: OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)

        val attemptNo = nextAttemptNo(task.id)
        val submission = jdbcClient.sql(
            """
            insert into submission (
              family_id, child_id, task_instance_id, attempt_no, submission_type, status,
              client_mutation_id, submitted_by_device_id, submitted_at
            ) values (
              :family_id, :child_id, :task_instance_id, :attempt_no, :submission_type, :status,
              :client_mutation_id, :submitted_by_device_id, :submitted_at
            )
            returning id, family_id, child_id, task_instance_id, attempt_no, submission_type, status, submitted_at
            """.trimIndent(),
        )
            .param("family_id", task.familyId)
            .param("child_id", task.childId)
            .param("task_instance_id", task.id)
            .param("attempt_no", attemptNo)
            .param("submission_type", task.submissionType)
            .param("status", "review_pending")
            .param("client_mutation_id", request.clientMutationId.trim())
            .param("submitted_by_device_id", user.deviceId)
            .param("submitted_at", submittedAt)
            .query(::submissionRecord)
            .single()

        media.forEachIndexed { index, asset ->
            jdbcClient.sql(
                """
                insert into submission_media (submission_id, media_asset_id, sort_order)
                values (:submission_id, :media_asset_id, :sort_order)
                """.trimIndent(),
            )
                .param("submission_id", submission.id)
                .param("media_asset_id", asset.id)
                .param("sort_order", index)
                .update()
        }

        supersedePreviousSubmission(task.latestSubmissionId, submission.id)
        updateTaskLatestSubmission(task.id, "pending_review", submission.id)
        publishSubmissionCreated(submission, media)
        return submission.toResponse()
    }

    fun getSubmission(submissionId: UUID): SubmissionDetailResponse {
        val user = currentUser.require()
        val submission = findSubmission(submissionId) ?: throw NotFoundError("Submission not found.")
        familyPolicy.requireCanAccessChild(user, submission.childId)
        val task = findTaskResponse(submission.taskInstanceId) ?: throw NotFoundError("Task not found.")
        val media = listSubmissionMedia(submissionId).map(mediaService::toResponse)
        val aiPrecheck = aiPrecheckService.findPrecheck(submissionId)?.let(aiPrecheckService::toResponse)
        val review = findActiveReview(submissionId)?.let(::reviewResponse)

        return SubmissionDetailResponse(
            id = submission.id,
            familyId = submission.familyId,
            childId = submission.childId,
            taskInstanceId = submission.taskInstanceId,
            attemptNo = submission.attemptNo,
            submissionType = submission.submissionType,
            status = submission.status,
            submittedAt = submission.submittedAt,
            task = task,
            media = media,
            aiPrecheck = aiPrecheck,
            review = review,
        )
    }

    private fun loadAndValidateMedia(mediaAssetIds: List<UUID>, task: TaskForSubmission): List<MediaAssetRecord> {
        val uniqueIds = mediaAssetIds.distinct()
        if (uniqueIds.size != mediaAssetIds.size) throw BadRequestError("mediaAssetIds cannot contain duplicates.")
        if (task.submissionType != "manual" && uniqueIds.isEmpty()) {
            throw BadRequestError("This task requires at least one media asset.")
        }
        if (task.submissionType == "manual" && uniqueIds.isNotEmpty()) {
            throw BadRequestError("Manual submissions cannot attach media assets.")
        }

        val assets = uniqueIds.map { mediaId ->
            mediaService.findMedia(mediaId) ?: throw NotFoundError("Media asset not found.")
        }
        assets.forEach { asset ->
            if (asset.familyId != task.familyId || asset.childId != task.childId) {
                throw ForbiddenError("Media asset does not belong to this task.")
            }
            if (asset.purpose != "submission") throw BadRequestError("Submission can only attach submission media.")
            if (asset.relatedType != "task_instance" || asset.relatedId != task.id) {
                throw ConflictError("Media asset is not scoped to this task.")
            }
            if (asset.status !in setOf("uploaded", "ready")) throw ConflictError("Media asset must be finalized before submission.")
            validateMediaContentType(asset, task.submissionType)
        }
        return assets
    }

    private fun validateMediaContentType(media: MediaAssetRecord, submissionType: String) {
        val contentType = media.contentType.substringBefore(";").lowercase()
        when (submissionType) {
            "photo" -> if (!contentType.startsWith("image/")) throw BadRequestError("Photo tasks require image media.")
            "audio" -> if (!contentType.startsWith("audio/")) throw BadRequestError("Audio tasks require audio media.")
            "video" -> if (!contentType.startsWith("video/")) throw BadRequestError("Video tasks require video media.")
        }
    }

    private fun findSubmissionByMutationId(familyId: UUID, clientMutationId: String): SubmissionRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, task_instance_id, attempt_no, submission_type, status, submitted_at
            from submission
            where family_id = :family_id
              and client_mutation_id = :client_mutation_id
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("client_mutation_id", clientMutationId.trim())
            .query(::submissionRecord)
            .optional()
            .orElse(null)

    private fun findSubmission(submissionId: UUID): SubmissionRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, task_instance_id, attempt_no, submission_type, status, submitted_at
            from submission
            where id = :id
            """.trimIndent(),
        )
            .param("id", submissionId)
            .query(::submissionRecord)
            .optional()
            .orElse(null)

    private fun findTaskForSubmission(taskId: UUID): TaskForSubmission? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, submission_type, require_review, status, latest_submission_id
            from task_instance
            where id = :id
            """.trimIndent(),
        )
            .param("id", taskId)
            .query { rs, _ ->
                TaskForSubmission(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    submissionType = rs.getString("submission_type"),
                    requireReview = rs.getBoolean("require_review"),
                    status = rs.getString("status"),
                    latestSubmissionId = rs.getObject("latest_submission_id", UUID::class.java),
                )
            }
            .optional()
            .orElse(null)

    private fun findTaskResponse(taskId: UUID): TaskInstanceResponse? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, scheduled_date, title, category, submission_type,
                   description, target_text, is_core, require_review, status, latest_submission_id
            from task_instance
            where id = :id
            """.trimIndent(),
        )
            .param("id", taskId)
            .query(::taskInstanceResponse)
            .optional()
            .orElse(null)

    private fun listSubmissionMedia(submissionId: UUID): List<MediaAssetRecord> =
        jdbcClient.sql(
            """
            select ma.id, ma.family_id, ma.child_id, ma.purpose, ma.storage_key, ma.content_type, ma.size_bytes,
                   ma.status, ma.related_type, ma.related_id
            from submission_media sm
            join media_asset ma on ma.id = sm.media_asset_id
            where sm.submission_id = :submission_id
            order by sm.sort_order
            """.trimIndent(),
        )
            .param("submission_id", submissionId)
            .query(::mediaAssetRecord)
            .list()

    private fun findActiveReview(submissionId: UUID): ReviewRecord? =
        jdbcClient.sql(
            """
            select
              r.id, r.family_id, r.child_id, r.submission_id, r.task_instance_id,
              r.decision, r.reviewed_by, r.created_at, r.revoked_at,
              f.emoji as feedback_emoji, f.text as feedback_text, f.audio_media_id
            from review r
            left join feedback f on f.review_id = r.id
            where r.submission_id = :submission_id
              and r.revoked_at is null
            """.trimIndent(),
        )
            .param("submission_id", submissionId)
            .query(::reviewRecord)
            .optional()
            .orElse(null)

    private fun reviewResponse(review: ReviewRecord): ReviewResponse =
        ReviewResponse(
            id = review.id,
            submissionId = review.submissionId,
            taskInstanceId = review.taskInstanceId,
            decision = review.decision,
            reviewedBy = review.reviewedBy,
            feedback = reviewFeedbackResponse(review),
            createdAt = review.createdAt,
        )

    private fun reviewFeedbackResponse(review: ReviewRecord): FeedbackResponse? {
        if (review.feedbackEmoji == null && review.feedbackText == null && review.audioMediaId == null) return null
        return FeedbackResponse(
            emoji = review.feedbackEmoji,
            text = review.feedbackText,
            audioMedia = review.audioMediaId?.let { mediaId ->
                mediaService.findMedia(mediaId)?.let(mediaService::toResponse)
            },
        )
    }

    private fun publishSubmissionCreated(submission: SubmissionRecord, media: List<MediaAssetRecord>) {
        val mediaJson = media.joinToString(prefix = "[", postfix = "]") { "\"${it.id}\"" }
        eventPublisher.publishFamilyEvent(
            familyId = submission.familyId,
            eventType = "submission.created",
            aggregateType = "submission",
            aggregateId = submission.id,
            payloadJson = """
            {
              "submissionId": "${submission.id}",
              "familyId": "${submission.familyId}",
              "childId": "${submission.childId}",
              "taskInstanceId": "${submission.taskInstanceId}",
              "attemptNo": ${submission.attemptNo},
              "submissionType": "${submission.submissionType}",
              "status": "${submission.status}",
              "mediaAssetIds": $mediaJson,
              "submittedAt": "${submission.submittedAt}"
            }
            """.trimIndent(),
        )
    }

    private fun nextAttemptNo(taskId: UUID): Int =
        jdbcClient.sql(
            """
            select coalesce(max(attempt_no), 0) + 1
            from submission
            where task_instance_id = :task_instance_id
            """.trimIndent(),
        )
            .param("task_instance_id", taskId)
            .query(Int::class.java)
            .single()

    private fun supersedePreviousSubmission(previousSubmissionId: UUID?, newSubmissionId: UUID) {
        if (previousSubmissionId == null) return
        jdbcClient.sql(
            """
            update submission
            set status = 'superseded',
                superseded_by_submission_id = :new_submission_id,
                updated_at = now()
            where id = :previous_submission_id
              and status not in ('approved', 'cancelled', 'superseded')
            """.trimIndent(),
        )
            .param("previous_submission_id", previousSubmissionId)
            .param("new_submission_id", newSubmissionId)
            .update()
    }

    private fun updateTaskLatestSubmission(taskId: UUID, status: String, submissionId: UUID) {
        jdbcClient.sql(
            """
            update task_instance
            set status = :status,
                latest_submission_id = :submission_id,
                version = version + 1,
                updated_at = now()
            where id = :task_id
            """.trimIndent(),
        )
            .param("task_id", taskId)
            .param("status", status)
            .param("submission_id", submissionId)
            .update()
    }
}

data class TaskForSubmission(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val submissionType: String,
    val requireReview: Boolean,
    val status: String,
    val latestSubmissionId: UUID?,
)
