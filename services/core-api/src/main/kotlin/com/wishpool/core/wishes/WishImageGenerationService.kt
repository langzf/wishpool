package com.wishpool.core.wishes

import com.wishpool.core.ai.AiWorkerClient
import com.wishpool.core.ai.AiWorkerWishImageGenerationRequest
import com.wishpool.core.ai.AiWorkerWishImageGenerationResponse
import com.wishpool.core.ai.ImageModelProviderService
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.media.MediaService
import com.wishpool.core.media.RelatedResource
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Base64
import java.util.UUID

@Service
class WishImageGenerationService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val imageModelProviderService: ImageModelProviderService,
    private val aiWorkerClient: AiWorkerClient,
    private val mediaService: MediaService,
    private val wishService: WishService,
) {
    @Transactional
    fun createJob(request: CreateWishImageGenerationRequest): WishImageGenerationJobResponse {
        val user = currentUser.require()
        familyPolicy.requireParent(user, request.familyId)
        familyPolicy.requireCanAccessChild(user, request.childId).also {
            if (it.familyId != request.familyId) throw BadRequestError("Child profile does not belong to this family.")
        }
        validateCreateRequest(request)
        if (request.wishId != null) ensureWishBelongsToChild(request.wishId, request.familyId, request.childId)
        val provider = imageModelProviderService.resolveProviderRecord(request.providerCode, request.usageCode)
        val job = jdbcClient.sql(
            """
            insert into wish_image_generation_job (
              family_id, child_id, wish_id, usage_code, provider_code, title_snapshot, note_snapshot,
              category, style, aspect_ratio, model_provider, model_name, created_by
            ) values (
              :family_id, :child_id, :wish_id, :usage_code, :provider_code, :title_snapshot, :note_snapshot,
              :category, :style, :aspect_ratio, :model_provider, :model_name, :created_by
            )
            returning ${selectColumns()}
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("child_id", request.childId)
            .param("wish_id", request.wishId)
            .param("usage_code", request.usageCode.trim().lowercase())
            .param("provider_code", provider?.code)
            .param("title_snapshot", request.title.trim())
            .param("note_snapshot", request.note?.trim())
            .param("category", request.category?.trim())
            .param("style", request.style)
            .param("aspect_ratio", request.aspectRatio)
            .param("model_provider", provider?.providerType)
            .param("model_name", provider?.modelName)
            .param("created_by", user.userId)
            .query(::wishImageGenerationJobRecord)
            .single()
        processQueuedJob(job)
        return getJob(job.id)
    }

    fun getJob(jobId: UUID): WishImageGenerationJobResponse {
        val job = jdbcClient.sql(
            """
            select ${selectColumns()}
            from wish_image_generation_job
            where id = :id
            """.trimIndent(),
        )
            .param("id", jobId)
            .query(::wishImageGenerationJobRecord)
            .optional()
            .orElseThrow { NotFoundError("Wish image generation job not found.") }
        familyPolicy.requireCanAccessChild(currentUser.require(), job.childId)
        return toResponse(job)
    }

    private fun validateCreateRequest(request: CreateWishImageGenerationRequest) {
        if (request.title.isBlank()) throw BadRequestError("title cannot be blank.")
        if (request.usageCode.isBlank()) throw BadRequestError("usageCode cannot be blank.")
        if (request.style !in setOf("warm_illustration", "storybook", "clean_product")) throw BadRequestError("Unsupported style.")
        if (request.aspectRatio !in setOf("1:1", "4:3")) throw BadRequestError("Unsupported aspectRatio.")
    }

    private fun ensureWishBelongsToChild(wishId: UUID, familyId: UUID, childId: UUID) {
        val count = jdbcClient.sql(
            """
            select count(*)
            from wish
            where id = :wish_id
              and family_id = :family_id
              and child_id = :child_id
            """.trimIndent(),
        )
            .param("wish_id", wishId)
            .param("family_id", familyId)
            .param("child_id", childId)
            .query(Int::class.java)
            .single()
        if (count != 1) throw NotFoundError("Wish not found.")
    }

    private fun toResponse(record: WishImageGenerationJobRecord): WishImageGenerationJobResponse =
        WishImageGenerationJobResponse(
            id = record.id,
            familyId = record.familyId,
            childId = record.childId,
            wishId = record.wishId,
            usageCode = record.usageCode,
            providerCode = record.providerCode,
            title = record.title,
            note = record.note,
            category = record.category,
            style = record.style,
            aspectRatio = record.aspectRatio,
            status = record.status,
            mediaAssetId = record.mediaAssetId,
            media = record.mediaAssetId?.let { mediaId -> mediaService.findMedia(mediaId)?.let(mediaService::toResponse) },
            modelProvider = record.modelProvider,
            modelName = record.modelName,
            attemptCount = record.attemptCount,
            errorCode = record.errorCode,
            errorMessage = record.errorMessage,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
        )

    private fun processQueuedJob(job: WishImageGenerationJobRecord) {
        val claimed = markJobRunning(job.id) ?: return
        try {
            val aiResponse = aiWorkerClient.generateWishImage(toWorkerRequest(claimed), usageCode = claimed.usageCode)
            val imageBytes = decodeGeneratedImage(aiResponse)
            val media = mediaService.createGeneratedWishImage(
                familyId = claimed.familyId,
                childId = claimed.childId,
                contentType = aiResponse.content_type,
                bytes = imageBytes,
                relatedResource = claimed.wishId?.let { RelatedResource(type = "wish", id = it) },
            )
            claimed.wishId?.let { wishId ->
                wishService.attachWishImage(wishId, AttachWishImageRequest(mediaId = media.id, sourceType = "generated"))
            }
            markJobSucceeded(claimed.id, media.id, aiResponse)
        } catch (ex: RuntimeException) {
            markJobFailed(claimed.id, errorCode(ex), errorMessage(ex))
        }
    }

    private fun markJobRunning(jobId: UUID): WishImageGenerationJobRecord? =
        jdbcClient.sql(
            """
            update wish_image_generation_job
            set status = 'running',
                attempt_count = attempt_count + 1,
                leased_until = now() + interval '5 minutes',
                error_code = null,
                error_message = null,
                updated_at = now()
            where id = :id
              and status = 'queued'
            returning ${selectColumns()}
            """.trimIndent(),
        )
            .param("id", jobId)
            .query(::wishImageGenerationJobRecord)
            .optional()
            .orElse(null)

    private fun markJobSucceeded(
        jobId: UUID,
        mediaAssetId: UUID,
        aiResponse: AiWorkerWishImageGenerationResponse,
    ) {
        jdbcClient.sql(
            """
            update wish_image_generation_job
            set status = 'succeeded',
                media_asset_id = :media_asset_id,
                model_provider = :model_provider,
                model_name = :model_name,
                prompt = :prompt,
                leased_until = null,
                error_code = null,
                error_message = null,
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", jobId)
            .param("media_asset_id", mediaAssetId)
            .param("model_provider", aiResponse.provider)
            .param("model_name", aiResponse.model)
            .param("prompt", aiResponse.prompt)
            .update()
    }

    private fun markJobFailed(jobId: UUID, errorCode: String, errorMessage: String) {
        jdbcClient.sql(
            """
            update wish_image_generation_job
            set status = 'failed_final',
                leased_until = null,
                error_code = :error_code,
                error_message = :error_message,
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", jobId)
            .param("error_code", errorCode.take(120))
            .param("error_message", errorMessage.take(1000))
            .update()
    }

    private fun toWorkerRequest(job: WishImageGenerationJobRecord): AiWorkerWishImageGenerationRequest =
        AiWorkerWishImageGenerationRequest(
            request_id = job.id.toString(),
            family_id = job.familyId.toString(),
            wish_title = job.title,
            wish_note = job.note,
            category = job.category,
            style = job.style,
            aspect_ratio = job.aspectRatio,
            provider_code = job.providerCode,
        )

    private fun decodeGeneratedImage(aiResponse: AiWorkerWishImageGenerationResponse): ByteArray {
        val encoded = aiResponse.image_base64?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("AI worker did not return image content.")
        return Base64.getDecoder().decode(encoded)
    }

    private fun errorCode(ex: RuntimeException): String =
        when (ex) {
            is IllegalArgumentException -> "invalid_ai_image"
            else -> "ai_worker_error"
        }

    private fun errorMessage(ex: RuntimeException): String =
        ex.message?.takeIf { it.isNotBlank() } ?: "AI image generation failed."

    private fun selectColumns(): String =
        """
        id, family_id, child_id, wish_id, usage_code, provider_code, title_snapshot, note_snapshot,
        category, style, aspect_ratio, status, media_asset_id, model_provider, model_name, prompt,
        attempt_count, error_code, error_message, created_at, updated_at
        """.trimIndent()
}
