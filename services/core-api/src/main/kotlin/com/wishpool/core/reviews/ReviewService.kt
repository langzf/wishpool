package com.wishpool.core.reviews

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.idempotency.IdempotencyService
import com.wishpool.core.media.MediaAssetRecord
import com.wishpool.core.media.MediaService
import com.wishpool.core.media.mediaAssetRecord
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.ForbiddenError
import com.wishpool.core.shared.NotFoundError
import com.wishpool.core.submissions.SubmissionDetailResponse
import com.wishpool.core.submissions.SubmissionRecord
import com.wishpool.core.submissions.SubmissionService
import com.wishpool.core.submissions.submissionRecord
import com.wishpool.core.tasks.TaskInstanceResponse
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ReviewService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
    private val submissionService: SubmissionService,
    private val eventPublisher: DomainEventPublisher,
    private val idempotencyService: IdempotencyService,
) {
    fun listPendingReviews(familyId: UUID): List<PendingReviewCardResponse> {
        familyPolicy.requireParent(currentUser.require(), familyId)
        return jdbcClient.sql(
            """
            select
              s.id as submission_id,
              s.family_id as submission_family_id,
              s.child_id as submission_child_id,
              s.task_instance_id,
              s.attempt_no,
              s.submission_type,
              s.status as submission_status,
              s.submitted_at,
              ti.id as task_id,
              ti.family_id as task_family_id,
              ti.child_id as task_child_id,
              ti.scheduled_date,
              ti.title,
              ti.category,
              ti.submission_type as task_submission_type,
              ti.description,
              ti.target_text,
              ti.is_core,
              ti.require_review,
              ti.status as task_status,
              ti.latest_submission_id,
              ap.summary as ai_summary,
              tm.id as thumb_id,
              tm.family_id as thumb_family_id,
              tm.child_id as thumb_child_id,
              tm.purpose as thumb_purpose,
              tm.storage_key as thumb_storage_key,
              tm.content_type as thumb_content_type,
              tm.size_bytes as thumb_size_bytes,
              tm.status as thumb_status,
              tm.related_type as thumb_related_type,
              tm.related_id as thumb_related_id
            from submission s
            join task_instance ti on ti.id = s.task_instance_id
            left join ai_precheck ap on ap.submission_id = s.id
            left join lateral (
              select ma.*
              from submission_media sm
              join media_asset ma on ma.id = sm.media_asset_id
              where sm.submission_id = s.id
              order by sm.sort_order
              limit 1
            ) tm on true
            where s.family_id = :family_id
              and s.status = 'review_pending'
              and ti.status = 'pending_review'
            order by s.submitted_at asc
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .query { rs, _ ->
                PendingReviewCardResponse(
                    submission = SubmissionRecord(
                        id = rs.getObject("submission_id", UUID::class.java),
                        familyId = rs.getObject("submission_family_id", UUID::class.java),
                        childId = rs.getObject("submission_child_id", UUID::class.java),
                        taskInstanceId = rs.getObject("task_instance_id", UUID::class.java),
                        attemptNo = rs.getInt("attempt_no"),
                        submissionType = rs.getString("submission_type"),
                        status = rs.getString("submission_status"),
                        submittedAt = rs.getObject("submitted_at", java.time.OffsetDateTime::class.java),
                    ).toResponse(),
                    task = TaskInstanceResponse(
                        id = rs.getObject("task_id", UUID::class.java),
                        familyId = rs.getObject("task_family_id", UUID::class.java),
                        childId = rs.getObject("task_child_id", UUID::class.java),
                        scheduledDate = rs.getDate("scheduled_date").toLocalDate(),
                        title = rs.getString("title"),
                        category = rs.getString("category"),
                        submissionType = rs.getString("task_submission_type"),
                        description = rs.getString("description"),
                        targetText = rs.getString("target_text"),
                        isCore = rs.getBoolean("is_core"),
                        requireReview = rs.getBoolean("require_review"),
                        status = rs.getString("task_status"),
                        latestSubmissionId = rs.getObject("latest_submission_id", UUID::class.java),
                    ),
                    aiSummary = rs.getString("ai_summary"),
                    thumbnailMedia = rs.getObject("thumb_id", UUID::class.java)?.let {
                        mediaService.toResponse(
                            MediaAssetRecord(
                                id = it,
                                familyId = rs.getObject("thumb_family_id", UUID::class.java),
                                childId = rs.getObject("thumb_child_id", UUID::class.java),
                                purpose = rs.getString("thumb_purpose"),
                                storageKey = rs.getString("thumb_storage_key"),
                                contentType = rs.getString("thumb_content_type"),
                                sizeBytes = rs.getLong("thumb_size_bytes").takeUnless { rs.wasNull() },
                                status = rs.getString("thumb_status"),
                                relatedType = rs.getString("thumb_related_type"),
                                relatedId = rs.getObject("thumb_related_id", UUID::class.java),
                            ),
                        )
                    },
                )
            }
            .list()
    }

    fun getReviewDetail(submissionId: UUID): SubmissionDetailResponse =
        submissionService.getSubmission(submissionId)

    @Transactional
    fun reviewSubmission(request: ReviewSubmissionRequest, idempotencyKey: String?): ReviewResponse {
        val user = currentUser.require()
        validateReviewRequest(request)
        val submission = findSubmissionForUpdate(request.submissionId) ?: throw NotFoundError("Submission not found.")
        familyPolicy.requireParent(user, submission.familyId)
        idempotencyService.find(submission.familyId, idempotencyKey, REVIEW_OPERATION)
            ?.let { return getReview(it.resourceId) }
        if (submission.status != "review_pending") throw ConflictError("Submission is not pending review.")

        val task = findTaskForUpdate(submission.taskInstanceId) ?: throw NotFoundError("Task not found.")
        if (task.latestSubmissionId != submission.id || task.status != "pending_review") {
            throw ConflictError("Submission is no longer the pending review target for this task.")
        }
        request.feedback?.audioMediaId?.let { validateFeedbackAudio(it, submission) }

        val createdReview = try {
            insertReview(submission, request.decision, user.userId)
        } catch (ex: DuplicateKeyException) {
            throw ConflictError("Submission has already been reviewed.")
        }
        request.feedback?.let { insertFeedback(createdReview.id, it) }
        val review = findReview(createdReview.id) ?: throw NotFoundError("Review not found.")
        applyReviewDecision(review, submission)
        publishReviewEvents(review, submission, request.feedback)
        audit(submission.familyId, user.userId, "review.${request.decision}", "review", review.id)
        idempotencyService.remember(submission.familyId, idempotencyKey, REVIEW_OPERATION, "review", review.id, user.userId)
        return toResponse(review)
    }

    @Transactional
    fun revokeReview(reviewId: UUID, request: RevokeReviewRequest, idempotencyKey: String?): ReviewResponse {
        val user = currentUser.require()
        if (request.reason.isBlank()) throw BadRequestError("reason cannot be blank.")
        val review = findReviewForUpdate(reviewId) ?: throw NotFoundError("Review not found.")
        familyPolicy.requireParent(user, review.familyId)
        idempotencyService.find(review.familyId, idempotencyKey, REVOKE_OPERATION)
            ?.let { return getReview(it.resourceId) }
        if (review.revokedAt != null) return toResponse(review)

        jdbcClient.sql(
            """
            update review
            set revoked_at = now(),
                revoke_reason = :reason
            where id = :id
            """.trimIndent(),
        )
            .param("id", reviewId)
            .param("reason", request.reason.trim())
            .update()

        if (review.decision == "approved") {
            jdbcClient.sql("update submission set status = 'review_pending', updated_at = now() where id = :id")
                .param("id", review.submissionId)
                .update()
            jdbcClient.sql(
                """
                update task_instance
                set status = 'pending_review',
                    approved_review_id = null,
                    version = version + 1,
                    updated_at = now()
                where id = :id
                  and approved_review_id = :review_id
                """.trimIndent(),
            )
                .param("id", review.taskInstanceId)
                .param("review_id", review.id)
                .update()
        }

        eventPublisher.publishFamilyEvent(
            familyId = review.familyId,
            eventType = "review.revoked",
            aggregateType = "review",
            aggregateId = review.id,
            payloadJson = """
            {
              "reviewId": "${review.id}",
              "familyId": "${review.familyId}",
              "childId": "${review.childId}",
              "submissionId": "${review.submissionId}",
              "taskInstanceId": "${review.taskInstanceId}",
              "actorUserId": "${user.userId}",
              "reviewedBy": "${review.reviewedBy}",
              "decision": "${review.decision}",
              "reason": ${jsonString(request.reason.trim())}
            }
            """.trimIndent(),
        )
        audit(review.familyId, user.userId, "review.revoke", "review", review.id)
        idempotencyService.remember(review.familyId, idempotencyKey, REVOKE_OPERATION, "review", review.id, user.userId)
        return getReview(reviewId)
    }

    fun toResponse(review: ReviewRecord): ReviewResponse =
        ReviewResponse(
            id = review.id,
            submissionId = review.submissionId,
            taskInstanceId = review.taskInstanceId,
            decision = review.decision,
            reviewedBy = review.reviewedBy,
            feedback = feedbackResponse(review),
            createdAt = review.createdAt,
        )

    private fun validateReviewRequest(request: ReviewSubmissionRequest) {
        if (request.decision !in setOf("approved", "needs_revision")) throw BadRequestError("Unsupported review decision.")
        request.feedback?.text?.let {
            if (it.length > 1000) throw BadRequestError("Feedback text is too long.")
        }
        request.feedback?.emoji?.let {
            if (it.length > 64) throw BadRequestError("Feedback emoji is too long.")
        }
    }

    private fun findSubmissionForUpdate(submissionId: UUID): SubmissionRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, task_instance_id, attempt_no, submission_type, status, submitted_at
            from submission
            where id = :id
            for update
            """.trimIndent(),
        )
            .param("id", submissionId)
            .query(::submissionRecord)
            .optional()
            .orElse(null)

    private fun findTaskForUpdate(taskId: UUID): TaskForReview? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, status, latest_submission_id
            from task_instance
            where id = :id
            for update
            """.trimIndent(),
        )
            .param("id", taskId)
            .query { rs, _ ->
                TaskForReview(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    status = rs.getString("status"),
                    latestSubmissionId = rs.getObject("latest_submission_id", UUID::class.java),
                )
            }
            .optional()
            .orElse(null)

    private fun validateFeedbackAudio(mediaId: UUID, submission: SubmissionRecord): MediaAssetRecord {
        val media = mediaService.findMedia(mediaId) ?: throw NotFoundError("Feedback audio media not found.")
        if (media.familyId != submission.familyId || media.childId != submission.childId) {
            throw ForbiddenError("Feedback audio media does not belong to this submission.")
        }
        if (media.purpose != "feedback") throw BadRequestError("Feedback audio must use feedback media purpose.")
        if (media.status !in setOf("uploaded", "ready")) throw ConflictError("Feedback audio media must be finalized.")
        if (!media.contentType.substringBefore(";").lowercase().startsWith("audio/")) {
            throw BadRequestError("Feedback audio must be an audio media asset.")
        }
        return media
    }

    private fun insertReview(submission: SubmissionRecord, decision: String, reviewedBy: UUID): ReviewRecord =
        jdbcClient.sql(
            """
            insert into review (
              family_id, child_id, submission_id, task_instance_id, decision, reviewed_by
            ) values (
              :family_id, :child_id, :submission_id, :task_instance_id, :decision, :reviewed_by
            )
            returning id
            """.trimIndent(),
        )
            .param("family_id", submission.familyId)
            .param("child_id", submission.childId)
            .param("submission_id", submission.id)
            .param("task_instance_id", submission.taskInstanceId)
            .param("decision", decision)
            .param("reviewed_by", reviewedBy)
            .query(UUID::class.java)
            .single()
            .let { reviewId -> findReview(reviewId) ?: throw NotFoundError("Review not found.") }

    private fun insertFeedback(reviewId: UUID, feedback: FeedbackInput) {
        if (feedback.emoji.isNullOrBlank() && feedback.text.isNullOrBlank() && feedback.audioMediaId == null) return
        jdbcClient.sql(
            """
            insert into feedback (review_id, emoji, text, audio_media_id)
            values (:review_id, :emoji, :text, :audio_media_id)
            """.trimIndent(),
        )
            .param("review_id", reviewId)
            .param("emoji", feedback.emoji?.trim())
            .param("text", feedback.text?.trim())
            .param("audio_media_id", feedback.audioMediaId)
            .update()
    }

    private fun applyReviewDecision(review: ReviewRecord, submission: SubmissionRecord) {
        val submissionStatus = if (review.decision == "approved") "approved" else "rejected"
        val taskStatus = if (review.decision == "approved") "approved" else "needs_revision"
        jdbcClient.sql(
            """
            update submission
            set status = :status,
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", submission.id)
            .param("status", submissionStatus)
            .update()

        jdbcClient.sql(
            """
            update task_instance
            set status = :status,
                approved_review_id = :approved_review_id,
                version = version + 1,
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", submission.taskInstanceId)
            .param("status", taskStatus)
            .param("approved_review_id", review.id.takeIf { review.decision == "approved" })
            .update()
    }

    private fun publishReviewEvents(review: ReviewRecord, submission: SubmissionRecord, feedback: FeedbackInput?) {
        val eventType = if (review.decision == "approved") "review.approved" else "review.revision_requested"
        eventPublisher.publishFamilyEvent(
            familyId = submission.familyId,
            eventType = eventType,
            aggregateType = "review",
            aggregateId = review.id,
            payloadJson = """
            {
              "reviewId": "${review.id}",
              "familyId": "${submission.familyId}",
              "childId": "${submission.childId}",
              "submissionId": "${submission.id}",
              "taskInstanceId": "${submission.taskInstanceId}",
              "actorUserId": "${review.reviewedBy}",
              "reviewedBy": "${review.reviewedBy}",
              "decision": "${review.decision}"
            }
            """.trimIndent(),
        )

        if (feedback != null && (!feedback.emoji.isNullOrBlank() || !feedback.text.isNullOrBlank() || feedback.audioMediaId != null)) {
            eventPublisher.publishFamilyEvent(
                familyId = submission.familyId,
                eventType = "feedback.created",
                aggregateType = "review",
                aggregateId = review.id,
                payloadJson = """
                {
                  "reviewId": "${review.id}",
                  "familyId": "${submission.familyId}",
                  "childId": "${submission.childId}",
                  "submissionId": "${submission.id}",
                  "taskInstanceId": "${submission.taskInstanceId}",
                  "hasText": ${!feedback.text.isNullOrBlank()},
                  "hasEmoji": ${!feedback.emoji.isNullOrBlank()},
                  "audioMediaId": ${feedback.audioMediaId?.let { "\"$it\"" } ?: "null"}
                }
                """.trimIndent(),
            )
        }
    }

    private fun getReview(reviewId: UUID): ReviewResponse =
        toResponse(
            findReview(reviewId) ?: throw NotFoundError("Review not found."),
        )

    private fun findReview(reviewId: UUID): ReviewRecord? =
        jdbcClient.sql(reviewSelectSql("where r.id = :id"))
            .param("id", reviewId)
            .query(::reviewRecord)
            .optional()
            .orElse(null)

    private fun findReviewForUpdate(reviewId: UUID): ReviewRecord? =
        jdbcClient.sql(
            """
            select id
            from review
            where id = :id
            for update
            """.trimIndent(),
        )
            .param("id", reviewId)
            .query(UUID::class.java)
            .optional()
            .orElse(null)
            ?.let { findReview(it) }

    private fun reviewSelectSql(whereClause: String): String =
        """
        select
          r.id, r.family_id, r.child_id, r.submission_id, r.task_instance_id,
          r.decision, r.reviewed_by, r.created_at, r.revoked_at,
          f.emoji as feedback_emoji, f.text as feedback_text, f.audio_media_id
        from review r
        left join feedback f on f.review_id = r.id
        $whereClause
        """.trimIndent()

    private fun feedbackResponse(review: ReviewRecord): FeedbackResponse? {
        if (review.feedbackEmoji == null && review.feedbackText == null && review.audioMediaId == null) return null
        return FeedbackResponse(
            emoji = review.feedbackEmoji,
            text = review.feedbackText,
            audioMedia = review.audioMediaId?.let { mediaId ->
                mediaService.findMedia(mediaId)?.let(mediaService::toResponse)
            },
        )
    }

    private fun audit(familyId: UUID, userId: UUID, action: String, resourceType: String, resourceId: UUID) {
        jdbcClient.sql(
            """
            insert into audit_log (
              family_id, actor_user_id, actor_role, action, resource_type, resource_id
            ) values (
              :family_id, :actor_user_id, 'parent', :action, :resource_type, :resource_id
            )
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("actor_user_id", userId)
            .param("action", action)
            .param("resource_type", resourceType)
            .param("resource_id", resourceId)
            .update()
    }

    private fun jsonString(value: String): String =
        buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }

    private companion object {
        const val REVIEW_OPERATION = "reviewSubmission"
        const val REVOKE_OPERATION = "revokeReview"
    }
}

data class TaskForReview(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val status: String,
    val latestSubmissionId: UUID?,
)
