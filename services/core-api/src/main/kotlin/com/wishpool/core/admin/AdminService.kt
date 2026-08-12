package com.wishpool.core.admin

import com.wishpool.core.media.MediaService
import com.wishpool.core.media.mediaAssetRecord
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class AdminService(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
    private val mediaService: MediaService,
    private val clock: Clock,
) {
    fun dashboard(): AdminDashboardResponse =
        AdminDashboardResponse(
            generatedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            familyCount = count("select count(*) from family where status <> 'deleted'"),
            activeChildCount = count("select count(*) from child_profile where status = 'active'"),
            pendingReviewCount = count("select count(*) from submission where status = 'pending_review'"),
            pendingNotificationCount = count("select count(*) from notification_event where status = 'pending'"),
            pendingOutboxCount = count("select count(*) from outbox_event where published_at is null"),
            processingMediaCount = count("select count(*) from media_asset where status = 'processing'"),
            runningAiJobCount = count("select count(*) from ai_job where status in ('queued', 'media_preparing', 'running')"),
            openPrivacyRequestCount = count(
                """
                select count(*)
                from privacy_request
                where status <> 'completed'
                """.trimIndent(),
            ),
        )

    fun listFamilies(limit: Int, status: String?): List<AdminFamilySummaryResponse> {
        val effectiveLimit = limit.coerceIn(1, 100)
        val statusClause = if (status.isNullOrBlank()) {
            ""
        } else {
            if (status !in FAMILY_STATUSES) throw BadRequestError("Unsupported family status.")
            "where f.status = :status"
        }
        val query = jdbcClient.sql(
            """
            select f.id, f.name, f.timezone, f.status, f.created_at,
                   count(distinct cp.id) as child_count,
                   count(distinct fm.id) as member_count
            from family f
            left join child_profile cp on cp.family_id = f.id and cp.status <> 'archived'
            left join family_member fm on fm.family_id = f.id and fm.status = 'active'
            $statusClause
            group by f.id, f.name, f.timezone, f.status, f.created_at
            order by f.created_at desc
            limit :limit
            """.trimIndent(),
        )
            .param("limit", effectiveLimit)
        return (if (status.isNullOrBlank()) query else query.param("status", status))
            .query(::adminFamilySummaryRecord)
            .list()
            .map(::toFamilySummary)
    }

    fun listPrivacyRequests(limit: Int, status: String?): List<AdminPrivacyRequestResponse> {
        val effectiveLimit = limit.coerceIn(1, 100)
        val statusClause = if (status.isNullOrBlank()) {
            ""
        } else {
            "where status = :status"
        }
        val query = jdbcClient.sql(
            """
            select id, family_id, request_type, status, requested_by, export_media_id,
                   reason, created_at, completed_at, error_message
            from privacy_request
            $statusClause
            order by created_at desc
            limit :limit
            """.trimIndent(),
        )
            .param("limit", effectiveLimit)
        return (if (status.isNullOrBlank()) query else query.param("status", status))
            .query(::adminPrivacyRequestRecord)
            .list()
            .map(::toPrivacyRequest)
    }

    fun listAuditLogs(familyId: UUID?, action: String?, limit: Int): List<AdminAuditLogResponse> {
        val effectiveLimit = limit.coerceIn(1, 100)
        val clauses = mutableListOf<String>()
        if (familyId != null) clauses += "family_id = :family_id"
        if (!action.isNullOrBlank()) clauses += "action = :action"
        val whereClause = if (clauses.isEmpty()) "" else "where ${clauses.joinToString(" and ")}"
        var query = jdbcClient.sql(
            """
            select id, family_id, actor_user_id, actor_role, action, resource_type, resource_id,
                   metadata_json, created_at
            from audit_log
            $whereClause
            order by created_at desc
            limit :limit
            """.trimIndent(),
        )
            .param("limit", effectiveLimit)
        if (familyId != null) query = query.param("family_id", familyId)
        if (!action.isNullOrBlank()) query = query.param("action", action)
        return query
            .query(adminAuditLogRecord(objectMapper))
            .list()
            .map(::toAuditLog)
    }

    @Transactional
    fun grantMediaAccess(request: AdminMediaAccessGrantRequest): AdminMediaAccessGrantResponse {
        if (request.reason.isBlank()) throw BadRequestError("reason cannot be blank.")
        val expiresInMinutes = request.expiresInMinutes.coerceIn(5, 120)
        val media = jdbcClient.sql(
            """
            select id, family_id, child_id, purpose, storage_key, content_type, size_bytes,
                   status, related_type, related_id
            from media_asset
            where id = :id
              and family_id = :family_id
              and status in ('uploaded', 'ready', 'processing')
            """.trimIndent(),
        )
            .param("id", request.mediaAssetId)
            .param("family_id", request.familyId)
            .query(::mediaAssetRecord)
            .optional()
            .orElseThrow { NotFoundError("Media asset not found.") }
        val auditLogId = UUID.randomUUID()
        jdbcClient.sql(
            """
            insert into audit_log (
              id, family_id, actor_role, action, resource_type, resource_id, metadata_json
            ) values (
              :id, :family_id, 'admin_support', 'admin.media_access_granted',
              'media_asset', :resource_id, cast(:metadata_json as jsonb)
            )
            """.trimIndent(),
        )
            .param("id", auditLogId)
            .param("family_id", request.familyId)
            .param("resource_id", request.mediaAssetId)
            .param(
                "metadata_json",
                objectMapper.writeValueAsString(
                    mapOf(
                        "reason" to request.reason.trim().take(500),
                        "expiresInMinutes" to expiresInMinutes,
                    ),
                ),
            )
            .update()
        val expiresAt = OffsetDateTime.ofInstant(clock.instant().plusSeconds(expiresInMinutes * 60), ZoneOffset.UTC)
        return AdminMediaAccessGrantResponse(
            mediaAssetId = media.id,
            familyId = media.familyId,
            accessUrl = mediaService.createDownloadUrl(media.storageKey),
            expiresAt = expiresAt,
            auditLogId = auditLogId,
        )
    }

    private fun count(sql: String): Long =
        jdbcClient.sql(sql)
            .query(Long::class.java)
            .single()

    private fun toFamilySummary(record: AdminFamilySummaryRecord): AdminFamilySummaryResponse =
        AdminFamilySummaryResponse(
            id = record.id,
            name = record.name,
            timezone = record.timezone,
            status = record.status,
            childCount = record.childCount,
            memberCount = record.memberCount,
            createdAt = record.createdAt,
        )

    private fun toPrivacyRequest(record: AdminPrivacyRequestRecord): AdminPrivacyRequestResponse =
        AdminPrivacyRequestResponse(
            id = record.id,
            familyId = record.familyId,
            requestType = record.requestType,
            status = record.status,
            requestedBy = record.requestedBy,
            exportMediaId = record.exportMediaId,
            reason = record.reason,
            createdAt = record.createdAt,
            completedAt = record.completedAt,
            errorMessage = record.errorMessage,
        )

    private fun toAuditLog(record: AdminAuditLogRecord): AdminAuditLogResponse =
        AdminAuditLogResponse(
            id = record.id,
            familyId = record.familyId,
            actorUserId = record.actorUserId,
            actorRole = record.actorRole,
            action = record.action,
            resourceType = record.resourceType,
            resourceId = record.resourceId,
            metadata = record.metadata,
            createdAt = record.createdAt,
        )

    private companion object {
        val FAMILY_STATUSES = setOf("active", "locked", "deleting", "deleted")
    }
}
