package com.wishpool.core.privacy

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.media.MediaService
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class PrivacyService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
    private val eventPublisher: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun createPrivacyRequest(request: PrivacyRequestCreate): PrivacyRequestResponse {
        val user = currentUser.require()
        familyPolicy.requireParent(user, request.familyId)
        if (request.requestType !in setOf("export", "delete")) throw BadRequestError("Unsupported privacy request type.")
        validateConfirmation(request)
        val exportMediaId = if (request.requestType == "export") createExportMedia(request.familyId, user.userId) else null
        val record = jdbcClient.sql(
            """
            insert into privacy_request (
              family_id, request_type, status, requested_by, export_media_id, reason
            ) values (
              :family_id, :request_type, 'requested', :requested_by, :export_media_id, :reason
            )
            returning id, family_id, request_type, status, requested_by, export_media_id, reason, created_at
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("request_type", request.requestType)
            .param("requested_by", user.userId)
            .param("export_media_id", exportMediaId)
            .param("reason", request.reason?.take(1000))
            .query(::privacyRequestRecord)
            .single()
        if (request.requestType == "delete") {
            jdbcClient.sql(
                """
                update family
                set status = 'locked',
                    updated_at = now()
                where id = :family_id
                  and status = 'active'
                """.trimIndent(),
            )
                .param("family_id", request.familyId)
                .update()
        }
        publishPrivacyEvent(record)
        return toResponse(record)
    }

    @Transactional
    fun runDeletionFromWorkflow(triggeredByEventId: UUID): PrivacyRequestResponse {
        val payload = outboxPayload(triggeredByEventId)
        val requestId = payload.path("privacyRequestId").asString().takeIf { it.isNotBlank() }?.let(UUID::fromString)
            ?: throw BadRequestError("privacyRequestId is required for privacy deletion.")
        val record = findForUpdate(requestId) ?: throw NotFoundError("Privacy request not found.")
        if (record.requestType != "delete") throw BadRequestError("Privacy deletion workflow requires a delete request.")

        updatePrivacyStatus(record.id, "verifying")
        updatePrivacyStatus(record.id, "locking_family")
        jdbcClient.sql("update family set status = 'deleting', updated_at = now() where id = :family_id")
            .param("family_id", record.familyId)
            .update()
        updatePrivacyStatus(record.id, "deleting_records")
        jdbcClient.sql("update child_profile set status = 'archived', updated_at = now() where family_id = :family_id")
            .param("family_id", record.familyId)
            .update()
        jdbcClient.sql("update family_member set status = 'removed', updated_at = now() where family_id = :family_id")
            .param("family_id", record.familyId)
            .update()
        updatePrivacyStatus(record.id, "deleting_objects")
        val storageKeys = jdbcClient.sql(
            """
            select storage_key from media_asset where family_id = :family_id
            union
            select md.storage_key
            from media_derivative md
            join media_asset ma on ma.id = md.media_asset_id
            where ma.family_id = :family_id
            """.trimIndent(),
        )
            .param("family_id", record.familyId)
            .query(String::class.java)
            .list()
        mediaService.deleteObjects(storageKeys.filterNotNull())
        jdbcClient.sql("update media_asset set status = 'deleted', updated_at = now() where family_id = :family_id")
            .param("family_id", record.familyId)
            .update()
        updatePrivacyStatus(record.id, "verifying_deletion")
        jdbcClient.sql("update family set status = 'deleted', updated_at = now() where id = :family_id")
            .param("family_id", record.familyId)
            .update()
        val completed = jdbcClient.sql(
            """
            update privacy_request
            set status = 'completed',
                completed_at = now()
            where id = :id
            returning id, family_id, request_type, status, requested_by, export_media_id, reason, created_at
            """.trimIndent(),
        )
            .param("id", record.id)
            .query(::privacyRequestRecord)
            .single()
        jdbcClient.sql(
            """
            insert into audit_log (
              family_id, actor_user_id, actor_role, action, resource_type, resource_id, metadata_json
            ) values (
              :family_id, :actor_user_id, 'system', 'privacy.delete.completed',
              'privacy_request', :resource_id, cast(:metadata_json as jsonb)
            )
            """.trimIndent(),
        )
            .param("family_id", completed.familyId)
            .param("actor_user_id", completed.requestedBy)
            .param("resource_id", completed.id)
            .param("metadata_json", objectMapper.writeValueAsString(mapOf("status" to "completed")))
            .update()
        eventPublisher.publishFamilyEvent(
            familyId = completed.familyId,
            eventType = "privacy.deletion_completed",
            aggregateType = "privacy_request",
            aggregateId = completed.id,
            payload = mapOf(
                "privacyRequestId" to completed.id.toString(),
                "familyId" to completed.familyId.toString(),
                "requestType" to completed.requestType,
                "status" to completed.status,
            ),
        )
        return toResponse(completed)
    }

    private fun validateConfirmation(request: PrivacyRequestCreate) {
        val normalized = request.confirmationText.trim()
        val expected = if (request.requestType == "delete") "DELETE FAMILY DATA" else "EXPORT FAMILY DATA"
        if (normalized != expected) {
            throw BadRequestError("confirmationText must be exactly '$expected'.")
        }
    }

    private fun createExportMedia(familyId: UUID, userId: UUID): UUID {
        val mediaId = UUID.randomUUID()
        jdbcClient.sql(
            """
            insert into media_asset (
              id, family_id, purpose, storage_key, content_type, status, created_by,
              related_type, related_id
            ) values (
              :id, :family_id, 'memory_export', :storage_key, 'application/zip',
              'upload_pending', :created_by, 'privacy_request', :related_id
            )
            """.trimIndent(),
        )
            .param("id", mediaId)
            .param("family_id", familyId)
            .param("storage_key", "families/$familyId/privacy_export/$mediaId.zip")
            .param("created_by", userId)
            .param("related_id", mediaId)
            .update()
        return mediaId
    }

    private fun publishPrivacyEvent(record: PrivacyRequestRecord) {
        val eventType = if (record.requestType == "delete") "privacy.deletion_requested" else "privacy.export_requested"
        eventPublisher.publishFamilyEvent(
            familyId = record.familyId,
            eventType = eventType,
            aggregateType = "privacy_request",
            aggregateId = record.id,
            payload = mapOf(
                "privacyRequestId" to record.id.toString(),
                "familyId" to record.familyId.toString(),
                "requestType" to record.requestType,
                "requestedBy" to record.requestedBy.toString(),
                "exportMediaId" to record.exportMediaId?.toString(),
            ),
        )
    }

    private fun toResponse(record: PrivacyRequestRecord): PrivacyRequestResponse =
        PrivacyRequestResponse(
            id = record.id,
            familyId = record.familyId,
            requestType = record.requestType,
            status = record.status,
            exportMedia = record.exportMediaId?.let { mediaService.findMedia(it)?.let(mediaService::toResponse) },
            createdAt = record.createdAt,
        )

    private fun outboxPayload(eventId: UUID): tools.jackson.databind.JsonNode =
        jdbcClient.sql("select payload_json::text from outbox_event where id = :id")
            .param("id", eventId)
            .query(String::class.java)
            .optional()
            .map(objectMapper::readTree)
            .orElseThrow { NotFoundError("Outbox event not found.") }

    private fun findForUpdate(requestId: UUID): PrivacyRequestRecord? =
        jdbcClient.sql(
            """
            select id, family_id, request_type, status, requested_by, export_media_id, reason, created_at
            from privacy_request
            where id = :id
            for update
            """.trimIndent(),
        )
            .param("id", requestId)
            .query(::privacyRequestRecord)
            .optional()
            .orElse(null)

    private fun updatePrivacyStatus(requestId: UUID, status: String) {
        jdbcClient.sql("update privacy_request set status = :status where id = :id")
            .param("id", requestId)
            .param("status", status)
            .update()
    }
}
