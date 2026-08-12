package com.wishpool.core.ai

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.media.MediaAssetRecord
import com.wishpool.core.media.MediaService
import com.wishpool.core.media.mediaAssetRecord
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.NotFoundError
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class AiPrecheckService(
    private val jdbcClient: JdbcClient,
    private val mediaService: MediaService,
    private val aiWorkerClient: AiWorkerClient,
    private val eventPublisher: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun runSubmissionPrecheck(request: RunAiPrecheckWorkflowRequest): AiPrecheckResponse {
        findPrecheck(request.submissionId)?.let { return toResponse(it) }
        val submission = findSubmissionContext(request.submissionId) ?: throw NotFoundError("Submission not found.")
        if (submission.status !in setOf("review_pending", "ai_pending", "ai_processing")) {
            throw ConflictError("Submission is not eligible for AI precheck.")
        }
        val media = listSubmissionMedia(request.submissionId)
        val jobId = createJob(submission)
        markJobRunning(jobId)
        val aiResponse = aiWorkerClient.precheckSubmission(toWorkerRequest(submission, media))
        val precheck = upsertPrecheck(submission, jobId, aiResponse)
        markJobSucceeded(jobId)
        eventPublisher.publishFamilyEvent(
            familyId = submission.familyId,
            eventType = "submission.ai_prechecked",
            aggregateType = "submission",
            aggregateId = submission.id,
            payload = mapOf(
                "submissionId" to submission.id.toString(),
                "familyId" to submission.familyId.toString(),
                "childId" to submission.childId.toString(),
                "taskInstanceId" to submission.taskInstanceId.toString(),
                "aiPrecheckId" to precheck.id.toString(),
                "summary" to precheck.summary,
                "confidence" to precheck.confidence,
            ),
        )
        return toResponse(precheck)
    }

    fun findPrecheck(submissionId: UUID): AiPrecheckRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, ai_job_id, submission_id, type, summary, confidence,
                   flags_json::text, model_provider, model_name, model_version, prompt_version, created_at
            from ai_precheck
            where submission_id = :submission_id
            """.trimIndent(),
        )
            .param("submission_id", submissionId)
            .query(aiPrecheckRecord(objectMapper))
            .optional()
            .orElse(null)

    fun toResponse(record: AiPrecheckRecord): AiPrecheckResponse =
        AiPrecheckResponse(
            id = record.id,
            submissionId = record.submissionId,
            type = record.type,
            summary = record.summary,
            confidence = record.confidence,
            flags = record.flags,
            model = mapOf(
                "provider" to record.modelProvider,
                "name" to record.modelName,
                "version" to record.modelVersion,
                "promptVersion" to record.promptVersion,
            ),
        )

    private fun findSubmissionContext(submissionId: UUID): SubmissionAiContext? =
        jdbcClient.sql(
            """
            select
              s.id, s.family_id, s.child_id, s.task_instance_id, s.submission_type, s.status,
              ti.title, ti.category,
              cp.birth_year
            from submission s
            join task_instance ti on ti.id = s.task_instance_id
            join child_profile cp on cp.id = s.child_id
            where s.id = :id
            for update
            """.trimIndent(),
        )
            .param("id", submissionId)
            .query { rs, _ ->
                val birthYear = rs.getInt("birth_year").takeUnless { rs.wasNull() }
                SubmissionAiContext(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    taskInstanceId = rs.getObject("task_instance_id", UUID::class.java),
                    submissionType = rs.getString("submission_type"),
                    status = rs.getString("status"),
                    taskTitle = rs.getString("title"),
                    taskCategory = rs.getString("category"),
                    birthYear = birthYear,
                )
            }
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

    private fun createJob(submission: SubmissionAiContext): UUID =
        jdbcClient.sql(
            """
            insert into ai_job (family_id, child_id, submission_id, job_type, status, model_route)
            values (:family_id, :child_id, :submission_id, :job_type, 'queued', 'submission-precheck')
            returning id
            """.trimIndent(),
        )
            .param("family_id", submission.familyId)
            .param("child_id", submission.childId)
            .param("submission_id", submission.id)
            .param("job_type", jobType(submission.submissionType))
            .query(UUID::class.java)
            .single()

    private fun markJobRunning(jobId: UUID) {
        jdbcClient.sql(
            """
            update ai_job
            set status = 'running',
                attempt_count = attempt_count + 1,
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", jobId)
            .update()
    }

    private fun markJobSucceeded(jobId: UUID) {
        jdbcClient.sql("update ai_job set status = 'succeeded', updated_at = now() where id = :id")
            .param("id", jobId)
            .update()
    }

    private fun upsertPrecheck(
        submission: SubmissionAiContext,
        jobId: UUID,
        aiResponse: AiWorkerPrecheckResponse,
    ): AiPrecheckRecord =
        try {
            insertPrecheck(submission, jobId, aiResponse)
        } catch (ex: DuplicateKeyException) {
            findPrecheck(submission.id) ?: throw ex
        }

    private fun insertPrecheck(
        submission: SubmissionAiContext,
        jobId: UUID,
        aiResponse: AiWorkerPrecheckResponse,
    ): AiPrecheckRecord {
        val flagsJson = objectMapper.writeValueAsString(
            listOf(
                mapOf("key" to "risk_level", "value" to aiResponse.risk_level),
                mapOf("key" to "suggested_decision", "value" to aiResponse.suggested_decision),
                mapOf("key" to "checklist", "value" to aiResponse.checklist),
                mapOf("key" to "safety_notes", "value" to aiResponse.safety_notes),
            ),
        )
        return jdbcClient.sql(
            """
            insert into ai_precheck (
              family_id, child_id, ai_job_id, submission_id, type, summary, confidence,
              flags_json, model_provider, model_name, model_version, prompt_version
            ) values (
              :family_id, :child_id, :ai_job_id, :submission_id, 'submission_precheck',
              :summary, :confidence, cast(:flags_json as jsonb), 'wishpool-ai-worker',
              'deterministic-precheck', '0.1.0', 'precheck-v1'
            )
            returning id, family_id, child_id, ai_job_id, submission_id, type, summary, confidence,
                      flags_json::text, model_provider, model_name, model_version, prompt_version, created_at
            """.trimIndent(),
        )
            .param("family_id", submission.familyId)
            .param("child_id", submission.childId)
            .param("ai_job_id", jobId)
            .param("submission_id", submission.id)
            .param("summary", aiResponse.summary)
            .param("confidence", aiResponse.confidence)
            .param("flags_json", flagsJson)
            .query(aiPrecheckRecord(objectMapper))
            .single()
    }

    private fun toWorkerRequest(submission: SubmissionAiContext, media: List<MediaAssetRecord>): AiWorkerPrecheckRequest =
        AiWorkerPrecheckRequest(
            submission_id = submission.id.toString(),
            task_title = submission.taskTitle,
            task_category = submission.taskCategory,
            child_age = null,
            media = media.map { asset ->
                AiWorkerMediaSignal(
                    media_id = asset.id.toString(),
                    kind = mediaKind(asset.contentType),
                    mime_type = asset.contentType,
                    visual_labels = listOf(asset.purpose, asset.status),
                )
            },
        )

    private fun jobType(submissionType: String): String =
        when (submissionType) {
            "photo" -> "image_homework_precheck"
            "audio" -> "reading_audio_precheck"
            "video" -> "exercise_video_precheck"
            else -> "content_safety"
        }

    private fun mediaKind(contentType: String): String =
        when {
            contentType.startsWith("image/") -> "image"
            contentType.startsWith("audio/") -> "audio"
            contentType.startsWith("video/") -> "video"
            else -> "text"
        }
}

private data class SubmissionAiContext(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val taskInstanceId: UUID,
    val submissionType: String,
    val status: String,
    val taskTitle: String,
    val taskCategory: String,
    val birthYear: Int?,
)
