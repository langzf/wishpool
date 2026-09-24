package com.wishpool.core.media

import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.security.AuthenticatedUser
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.ForbiddenError
import com.wishpool.core.shared.NotFoundError
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@Service
class MediaService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val eventPublisher: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
    private val s3Client: S3Client,
    @Qualifier("internalS3Presigner") private val internalS3Presigner: S3Presigner,
    @Qualifier("publicS3Presigner") private val publicS3Presigner: S3Presigner,
    private val clock: Clock,
    @Value("\${wishpool.storage.s3.bucket}") private val bucket: String,
    @Value("\${wishpool.storage.s3.upload-url-ttl-sec}") private val uploadUrlTtlSec: Long,
    @Value("\${wishpool.storage.s3.download-url-ttl-sec}") private val downloadUrlTtlSec: Long,
    @Value("\${wishpool.storage.s3.max-upload-size-bytes}") private val maxUploadSizeBytes: Long,
) {
    @Transactional
    fun createUploadSession(request: CreateUploadSessionRequest): UploadSessionResponse {
        val user = currentUser.require()
        validateCreateRequest(request)
        requireMediaAccess(user, request.familyId, request.childId, request.purpose)
        validateRelatedResource(request)

        val mediaId = UUID.randomUUID()
        val storageKey = storageKey(request.familyId, request.childId, request.purpose, mediaId, request.contentType)
        val expiresAt = OffsetDateTime.ofInstant(clock.instant().plusSeconds(uploadUrlTtlSec), ZoneOffset.UTC)

        jdbcClient.sql(
            """
            insert into media_asset (
              id, family_id, child_id, purpose, storage_key, content_type, size_bytes,
              related_type, related_id, status, created_by
            ) values (
              :id, :family_id, :child_id, :purpose, :storage_key, :content_type, :size_bytes,
              :related_type, :related_id, 'upload_pending', :created_by
            )
            """.trimIndent(),
        )
            .param("id", mediaId)
            .param("family_id", request.familyId)
            .param("child_id", request.childId)
            .param("purpose", request.purpose)
            .param("storage_key", storageKey)
            .param("content_type", request.contentType.trim().lowercase(Locale.ROOT))
            .param("size_bytes", request.sizeBytes)
            .param("related_type", request.relatedResource?.type)
            .param("related_id", request.relatedResource?.id)
            .param("created_by", user.userId)
            .update()

        return UploadSessionResponse(
            mediaId = mediaId,
            uploadUrl = presignedPutUrl(storageKey, request.contentType.trim().lowercase(Locale.ROOT), request.sizeBytes),
            storageKey = storageKey,
            expiresAt = expiresAt,
            maxSizeBytes = maxUploadSizeBytes,
        )
    }

    @Transactional
    fun finalizeMedia(mediaId: UUID, request: FinalizeMediaRequest): MediaAssetResponse {
        val user = currentUser.require()
        validateFinalizeRequest(request)
        val media = findMedia(mediaId) ?: throw NotFoundError("Media asset not found.")
        requireMediaAccess(user, media.familyId, media.childId, media.purpose)
        if (media.status !in setOf("upload_pending", "uploaded")) {
            throw BadRequestError("Only pending or uploaded media can be finalized.")
        }

        val objectMetadata = headUploadedObject(media)
        if (media.sizeBytes != null && objectMetadata.contentLength() != media.sizeBytes) {
            throw BadRequestError("Uploaded object size does not match the upload session.")
        }
        val uploadedContentType = objectMetadata.contentType()
        if (!uploadedContentType.isNullOrBlank() && !sameContentType(uploadedContentType, media.contentType)) {
            throw BadRequestError("Uploaded object content type does not match the upload session.")
        }

        val finalized = jdbcClient.sql(
            """
            update media_asset
            set checksum_sha256 = :checksum_sha256,
                width = :width,
                height = :height,
                duration_sec = :duration_sec,
                status = 'uploaded',
                updated_at = now()
            where id = :id
            returning id, family_id, child_id, purpose, storage_key, content_type, size_bytes, status, related_type, related_id
            """.trimIndent(),
        )
            .param("id", mediaId)
            .param("checksum_sha256", request.checksumSha256)
            .param("width", request.width)
            .param("height", request.height)
            .param("duration_sec", request.durationSec)
            .query(::mediaAssetRecord)
            .single()

        eventPublisher.publishFamilyEvent(
            familyId = finalized.familyId,
            eventType = "media.uploaded",
            aggregateType = "media_asset",
            aggregateId = finalized.id,
            payload = mediaEventPayload(finalized),
        )

        return toResponse(finalized)
    }

    fun createGeneratedWishImage(
        familyId: UUID,
        childId: UUID,
        contentType: String,
        bytes: ByteArray,
        relatedResource: RelatedResource? = null,
    ): MediaAssetResponse {
        val user = currentUser.require()
        val normalizedContentType = contentType.substringBefore(";").trim().lowercase(Locale.ROOT)
        if (normalizedContentType.isBlank()) throw BadRequestError("contentType cannot be blank.")
        if (!normalizedContentType.startsWith("image/")) throw BadRequestError("Generated wish image must be an image.")
        if (bytes.isEmpty()) throw BadRequestError("Generated wish image cannot be empty.")
        if (bytes.size > maxUploadSizeBytes) throw BadRequestError("Generated wish image exceeds the configured size limit.")
        requireMediaAccess(user, familyId, childId, "wish_image")
        relatedResource?.let {
            if (it.type !in allowedRelatedTypes) throw BadRequestError("Unsupported related resource type.")
        }

        val mediaId = UUID.randomUUID()
        val storageKey = storageKey(familyId, childId, "wish_image", mediaId, normalizedContentType)
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(normalizedContentType)
                .contentLength(bytes.size.toLong())
                .build(),
            RequestBody.fromBytes(bytes),
        )

        val media = jdbcClient.sql(
            """
            insert into media_asset (
              id, family_id, child_id, purpose, storage_key, content_type, size_bytes,
              related_type, related_id, status, created_by
            ) values (
              :id, :family_id, :child_id, 'wish_image', :storage_key, :content_type, :size_bytes,
              :related_type, :related_id, 'uploaded', :created_by
            )
            returning id, family_id, child_id, purpose, storage_key, content_type, size_bytes, status, related_type, related_id
            """.trimIndent(),
        )
            .param("id", mediaId)
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("storage_key", storageKey)
            .param("content_type", normalizedContentType)
            .param("size_bytes", bytes.size.toLong())
            .param("related_type", relatedResource?.type)
            .param("related_id", relatedResource?.id)
            .param("created_by", user.userId)
            .query(::mediaAssetRecord)
            .single()

        eventPublisher.publishFamilyEvent(
            familyId = media.familyId,
            eventType = "media.uploaded",
            aggregateType = "media_asset",
            aggregateId = media.id,
            payload = mediaEventPayload(media),
        )
        return toResponse(media)
    }

    @Transactional
    fun markProcessingStarted(mediaId: UUID): MediaAssetResponse {
        val media = findMedia(mediaId) ?: throw NotFoundError("Media asset not found.")
        if (media.status !in setOf("uploaded", "processing")) {
            throw ConflictError("Media asset is not ready for processing.")
        }
        val processing = updateMediaStatus(mediaId, "processing")
        eventPublisher.publishFamilyEvent(
            familyId = processing.familyId,
            eventType = "media.processing_started",
            aggregateType = "media_asset",
            aggregateId = processing.id,
            payload = mediaEventPayload(processing),
        )
        return toResponse(processing)
    }

    @Transactional
    fun claimProcessingItems(request: ClaimMediaProcessingRequest): ClaimMediaProcessingResponse {
        val limit = request.limit.coerceIn(1, 50)
        val leaseSeconds = request.leaseSeconds.coerceIn(30, 3600)
        val claimed = jdbcClient.sql(
            """
            with candidate as (
              select id
              from media_asset
              where status in ('uploaded', 'processing', 'failed')
                and processing_retryable = true
                and processing_available_at <= now()
                and (processing_leased_until is null or processing_leased_until < now())
              order by created_at
              limit cast(:limit as integer)
              for update skip locked
            )
            update media_asset ma
            set status = 'processing',
                processing_attempt_count = processing_attempt_count + 1,
                processing_leased_until = now() + (:lease_seconds || ' seconds')::interval,
                updated_at = now()
            from candidate
            where ma.id = candidate.id
            returning ma.id, ma.family_id, ma.child_id, ma.purpose, ma.storage_key, ma.content_type,
                      ma.size_bytes, ma.status, ma.related_type, ma.related_id
            """.trimIndent(),
        )
            .param("limit", limit)
            .param("lease_seconds", leaseSeconds)
            .query(::mediaAssetRecord)
            .list()
        claimed.forEach { media ->
            eventPublisher.publishFamilyEvent(
                familyId = media.familyId,
                eventType = "media.processing_started",
                aggregateType = "media_asset",
                aggregateId = media.id,
                payload = mediaEventPayload(media),
            )
        }
        return ClaimMediaProcessingResponse(
            items = claimed.map { media ->
                MediaProcessingSourceResponse(
                    media = toResponse(media, internalS3Presigner),
                    derivatives = listDerivatives(media.id).map(::toDerivativeResponse),
                    processing = processingPolicy(media),
                )
            },
        )
    }

    fun getProcessingSource(mediaId: UUID): MediaProcessingSourceResponse {
        val media = findMedia(mediaId) ?: throw NotFoundError("Media asset not found.")
        if (media.status !in setOf("uploaded", "processing", "ready")) {
            throw ConflictError("Media asset has not been finalized for processing.")
        }
        return MediaProcessingSourceResponse(
            media = toResponse(media, internalS3Presigner),
            derivatives = listDerivatives(mediaId).map(::toDerivativeResponse),
            processing = processingPolicy(media),
        )
    }

    @Transactional
    fun completeProcessing(mediaId: UUID, request: CompleteMediaProcessingRequest): MediaAssetResponse {
        val media = findMedia(mediaId) ?: throw NotFoundError("Media asset not found.")
        if (media.status !in setOf("uploaded", "processing", "ready")) {
            throw ConflictError("Media asset is not ready for processing completion.")
        }
        if (request.derivatives.isEmpty()) throw BadRequestError("At least one derivative is required.")
        validateDerivatives(media, request.derivatives)
        request.derivatives.forEach { upsertDerivative(mediaId, it) }
        val ready = updateMediaStatus(mediaId, "ready")
        clearProcessingLease(mediaId)
        eventPublisher.publishFamilyEvent(
            familyId = ready.familyId,
            eventType = "media.processing_completed",
            aggregateType = "media_asset",
            aggregateId = ready.id,
            payload = mediaEventPayload(ready) + mapOf("derivativeKinds" to request.derivatives.map { it.kind }),
        )
        return toResponse(ready)
    }

    @Transactional
    fun failProcessing(mediaId: UUID, request: FailMediaProcessingRequest): MediaAssetResponse {
        val media = findMedia(mediaId) ?: throw NotFoundError("Media asset not found.")
        if (request.errorCode.isBlank()) throw BadRequestError("errorCode cannot be blank.")
        if (request.errorMessage.isBlank()) throw BadRequestError("errorMessage cannot be blank.")
        val failed = failMediaStatus(mediaId, request)
        eventPublisher.publishFamilyEvent(
            familyId = failed.familyId,
            eventType = "media.processing_failed",
            aggregateType = "media_asset",
            aggregateId = failed.id,
            payload = mediaEventPayload(failed) + mapOf(
                "errorCode" to request.errorCode.take(120),
                "errorMessage" to request.errorMessage.take(1000),
                "retryable" to request.retryable,
            ),
        )
        return toResponse(failed)
    }

    fun findMedia(mediaId: UUID): MediaAssetRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, purpose, storage_key, content_type, size_bytes, status
                   , related_type, related_id
            from media_asset
            where id = :id
            """.trimIndent(),
        )
            .param("id", mediaId)
            .query(::mediaAssetRecord)
            .optional()
            .orElse(null)

    fun toResponse(media: MediaAssetRecord): MediaAssetResponse =
        toResponse(media, publicS3Presigner)

    private fun toResponse(media: MediaAssetRecord, presigner: S3Presigner): MediaAssetResponse =
        MediaAssetResponse(
            id = media.id,
            familyId = media.familyId,
            childId = media.childId,
            purpose = media.purpose,
            storageKey = media.storageKey,
            contentType = media.contentType,
            status = media.status,
            downloadUrl = media.takeIf { it.status in setOf("uploaded", "processing", "ready", "failed") }
                ?.let { presignedGetUrl(it.storageKey, presigner) },
        )

    fun createDownloadUrl(storageKey: String): String =
        presignedGetUrl(storageKey, publicS3Presigner)

    fun deleteObjects(storageKeys: Collection<String>) {
        storageKeys.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { storageKey ->
                s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .build(),
                )
            }
    }

    fun listDerivatives(mediaId: UUID): List<MediaDerivativeRecord> =
        jdbcClient.sql(
            """
            select id, media_asset_id, kind, storage_key, content_type, size_bytes,
                   metadata_json::text, created_at
            from media_derivative
            where media_asset_id = :media_id
            order by created_at, kind
            """.trimIndent(),
        )
            .param("media_id", mediaId)
            .query(mediaDerivativeRecord(objectMapper))
            .list()

    fun toDerivativeResponse(derivative: MediaDerivativeRecord): MediaDerivativeResponse =
        MediaDerivativeResponse(
            id = derivative.id,
            mediaAssetId = derivative.mediaAssetId,
            kind = derivative.kind,
            storageKey = derivative.storageKey,
            contentType = derivative.contentType,
            sizeBytes = derivative.sizeBytes,
            metadata = derivative.metadata,
            createdAt = derivative.createdAt,
        )

    private fun validateCreateRequest(request: CreateUploadSessionRequest) {
        if (request.purpose !in allowedPurposes) throw BadRequestError("Unsupported media purpose.")
        if (request.purpose == "submission" && request.childId == null) {
            throw BadRequestError("Submission media must be scoped to a child profile.")
        }
        if (request.purpose == "submission" && request.relatedResource?.type != "task_instance") {
            throw BadRequestError("Submission media must be scoped to a task instance.")
        }
        if (request.contentType.isBlank()) throw BadRequestError("contentType cannot be blank.")
        if (request.sizeBytes <= 0) throw BadRequestError("sizeBytes must be positive.")
        if (request.sizeBytes > maxUploadSizeBytes) throw BadRequestError("Upload size exceeds the configured limit.")
    }

    private fun validateRelatedResource(request: CreateUploadSessionRequest) {
        val related = request.relatedResource ?: return
        if (related.type !in allowedRelatedTypes) throw BadRequestError("Unsupported related resource type.")
        if (request.purpose != "submission") return

        val task = jdbcClient.sql(
            """
            select id, family_id, child_id, submission_type, status
            from task_instance
            where id = :id
            """.trimIndent(),
        )
            .param("id", related.id)
            .query { rs, _ ->
                UploadTaskRecord(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    submissionType = rs.getString("submission_type"),
                    status = rs.getString("status"),
                )
            }
            .optional()
            .orElseThrow { NotFoundError("Task not found.") }

        if (task.familyId != request.familyId || task.childId != request.childId) {
            throw BadRequestError("Task does not match the requested media scope.")
        }
        if (task.status !in setOf("todo", "needs_revision", "submitted")) {
            throw ConflictError("Task is not open for submission media.")
        }
        validateContentTypeForSubmission(request.contentType, task.submissionType)
    }

    private fun validateFinalizeRequest(request: FinalizeMediaRequest) {
        if (request.checksumSha256 != null && !sha256Pattern.matches(request.checksumSha256)) {
            throw BadRequestError("checksumSha256 must be a lowercase SHA-256 hex digest.")
        }
        if (request.width != null && request.width <= 0) throw BadRequestError("width must be positive.")
        if (request.height != null && request.height <= 0) throw BadRequestError("height must be positive.")
        if (request.durationSec != null && request.durationSec <= 0) throw BadRequestError("durationSec must be positive.")
    }

    private fun validateDerivatives(media: MediaAssetRecord, derivatives: List<MediaDerivativeInput>) {
        val requiredKinds = requiredDerivativeKinds(media.contentType)
        val kinds = derivatives.map { it.kind }
        if (kinds.distinct().size != kinds.size) throw BadRequestError("Derivative kinds cannot contain duplicates.")
        val unsupported = kinds.filter { it !in allowedDerivativeKinds }
        if (unsupported.isNotEmpty()) throw BadRequestError("Unsupported derivative kind.")
        val missing = requiredKinds.filterNot(kinds::contains)
        if (missing.isNotEmpty()) throw BadRequestError("Missing required derivative kind: ${missing.joinToString(",")}.")
        derivatives.forEach { derivative ->
            if (derivative.storageKey.isBlank()) throw BadRequestError("Derivative storageKey cannot be blank.")
            if (derivative.contentType.isBlank()) throw BadRequestError("Derivative contentType cannot be blank.")
            if (derivative.sizeBytes != null && derivative.sizeBytes <= 0) throw BadRequestError("Derivative sizeBytes must be positive.")
            if (!derivative.storageKey.startsWith(derivativePrefix(media))) {
                throw BadRequestError("Derivative storageKey must be scoped under the source media derivative prefix.")
            }
        }
    }

    private fun upsertDerivative(mediaId: UUID, derivative: MediaDerivativeInput) {
        jdbcClient.sql(
            """
            insert into media_derivative (
              media_asset_id, kind, storage_key, content_type, size_bytes, metadata_json
            ) values (
              :media_asset_id, :kind, :storage_key, :content_type, :size_bytes, cast(:metadata_json as jsonb)
            )
            on conflict (media_asset_id, kind) do update set
              storage_key = excluded.storage_key,
              content_type = excluded.content_type,
              size_bytes = excluded.size_bytes,
              metadata_json = excluded.metadata_json,
              created_at = now()
            """.trimIndent(),
        )
            .param("media_asset_id", mediaId)
            .param("kind", derivative.kind)
            .param("storage_key", derivative.storageKey)
            .param("content_type", derivative.contentType.trim().lowercase(Locale.ROOT))
            .param("size_bytes", derivative.sizeBytes)
            .param("metadata_json", objectMapper.writeValueAsString(derivative.metadata))
            .update()
    }

    private fun updateMediaStatus(mediaId: UUID, status: String): MediaAssetRecord =
        jdbcClient.sql(
            """
            update media_asset
            set status = :status,
                updated_at = now()
            where id = :id
            returning id, family_id, child_id, purpose, storage_key, content_type, size_bytes, status, related_type, related_id
            """.trimIndent(),
        )
            .param("id", mediaId)
            .param("status", status)
            .query(::mediaAssetRecord)
            .single()

    private fun failMediaStatus(mediaId: UUID, request: FailMediaProcessingRequest): MediaAssetRecord =
        jdbcClient.sql(
            """
            update media_asset
            set status = 'failed',
                processing_available_at = now() + (:delay_seconds || ' seconds')::interval,
                processing_leased_until = null,
                processing_retryable = :retryable,
                processing_last_error_code = :error_code,
                processing_last_error_message = :error_message,
                updated_at = now()
            where id = :id
            returning id, family_id, child_id, purpose, storage_key, content_type, size_bytes, status, related_type, related_id
            """.trimIndent(),
        )
            .param("id", mediaId)
            .param("delay_seconds", request.delaySeconds?.coerceIn(0, 86_400) ?: 60)
            .param("retryable", request.retryable)
            .param("error_code", request.errorCode.take(120))
            .param("error_message", request.errorMessage.take(1000))
            .query(::mediaAssetRecord)
            .single()

    private fun clearProcessingLease(mediaId: UUID) {
        jdbcClient.sql(
            """
            update media_asset
            set processing_leased_until = null,
                processing_retryable = false,
                processing_last_error_code = null,
                processing_last_error_message = null,
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", mediaId)
            .update()
    }

    private fun processingPolicy(media: MediaAssetRecord): MediaProcessingPolicyResponse =
        MediaProcessingPolicyResponse(
            requiredKinds = requiredDerivativeKinds(media.contentType),
            sourceBucket = bucket,
            attemptCount = jdbcClient.sql("select processing_attempt_count from media_asset where id = :id")
                .param("id", media.id)
                .query(Int::class.java)
                .single(),
        )

    private fun mediaEventPayload(media: MediaAssetRecord): Map<String, Any?> =
        mapOf(
            "mediaAssetId" to media.id.toString(),
            "familyId" to media.familyId.toString(),
            "childId" to media.childId?.toString(),
            "purpose" to media.purpose,
            "contentType" to media.contentType,
            "status" to media.status,
            "relatedType" to media.relatedType,
            "relatedId" to media.relatedId?.toString(),
        )

    private fun requiredDerivativeKinds(contentType: String): List<String> {
        val normalized = contentType.substringBefore(";").trim().lowercase(Locale.ROOT)
        return when {
            normalized.startsWith("image/") -> listOf("thumbnail", "preview", "ai_ready")
            normalized.startsWith("audio/") -> listOf("waveform", "transcoded", "ai_ready")
            normalized.startsWith("video/") -> listOf("thumbnail", "preview", "keyframe", "transcoded", "ai_ready")
            else -> emptyList()
        }
    }

    private fun requireMediaAccess(user: AuthenticatedUser, familyId: UUID, childId: UUID?, purpose: String) {
        val member = if (childId == null) {
            familyPolicy.requireMember(user, familyId)
        } else {
            val member = familyPolicy.requireCanAccessChild(user, childId)
            if (member.familyId != familyId) throw BadRequestError("Child profile does not belong to this family.")
            member
        }

        if (member.role == "child_device" && (childId == null || purpose != "submission")) {
            throw ForbiddenError("Child devices can only upload submission media for their own child profile.")
        }
        if (purpose != "submission" && member.role !in setOf("parent_owner", "parent")) {
            throw ForbiddenError("Parent permission is required for this media purpose.")
        }
    }

    private fun storageKey(
        familyId: UUID,
        childId: UUID?,
        purpose: String,
        mediaId: UUID,
        contentType: String,
    ): String {
        val childSegment = childId?.let { "/children/$it" } ?: ""
        return "families/$familyId$childSegment/$purpose/$mediaId.${extensionFor(contentType)}"
    }

    private fun derivativePrefix(media: MediaAssetRecord): String =
        media.storageKey.substringBeforeLast('.', media.storageKey) + "/derivatives/"

    private fun extensionFor(contentType: String): String =
        when (contentType.trim().lowercase(Locale.ROOT).substringBefore(";")) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/svg+xml" -> "svg"
            "image/heic" -> "heic"
            "audio/mpeg" -> "mp3"
            "audio/mp4" -> "m4a"
            "audio/aac" -> "aac"
            "audio/wav" -> "wav"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            else -> "bin"
        }

    private fun validateContentTypeForSubmission(contentType: String, submissionType: String) {
        val normalized = contentType.substringBefore(";").trim().lowercase(Locale.ROOT)
        when (submissionType) {
            "photo" -> if (!normalized.startsWith("image/")) throw BadRequestError("Photo tasks require image media.")
            "audio" -> if (!normalized.startsWith("audio/")) throw BadRequestError("Audio tasks require audio media.")
            "video" -> if (!normalized.startsWith("video/")) throw BadRequestError("Video tasks require video media.")
            "manual" -> throw BadRequestError("Manual tasks do not accept media uploads.")
        }
    }

    private fun presignedPutUrl(storageKey: String, contentType: String, sizeBytes: Long): String {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(storageKey)
            .contentType(contentType)
            .contentLength(sizeBytes)
            .build()
        return publicS3Presigner.presignPutObject {
            it.signatureDuration(Duration.ofSeconds(uploadUrlTtlSec))
                .putObjectRequest(request)
        }.url().toString()
    }

    private fun presignedGetUrl(storageKey: String, presigner: S3Presigner): String {
        val request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(storageKey)
            .build()
        return presigner.presignGetObject {
            it.signatureDuration(Duration.ofSeconds(downloadUrlTtlSec))
                .getObjectRequest(request)
        }.url().toString()
    }

    private fun headUploadedObject(media: MediaAssetRecord): software.amazon.awssdk.services.s3.model.HeadObjectResponse =
        try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(media.storageKey)
                    .build(),
            )
        } catch (ex: AwsServiceException) {
            if (ex.statusCode() == 404) throw BadRequestError("Uploaded object was not found in object storage.")
            throw ex
        } catch (ex: DataAccessException) {
            throw ex
        }

    private fun sameContentType(left: String, right: String): Boolean =
        left.substringBefore(";").trim().equals(right.substringBefore(";").trim(), ignoreCase = true)

    private companion object {
        val allowedPurposes = setOf("submission", "feedback", "wish_image", "wish_redemption", "memory_export")
        val allowedRelatedTypes = setOf("task_instance", "submission", "review", "wish", "wish_redemption", "weekly_memory")
        val allowedDerivativeKinds = setOf("thumbnail", "preview", "waveform", "transcoded", "keyframe", "ai_ready")
        val sha256Pattern = Regex("^[a-f0-9]{64}$")
    }
}

data class UploadTaskRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val submissionType: String,
    val status: String,
)
