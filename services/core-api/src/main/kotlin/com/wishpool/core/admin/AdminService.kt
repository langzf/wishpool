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
            limit cast(:limit as integer)
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
            limit cast(:limit as integer)
            """.trimIndent(),
        )
            .param("limit", effectiveLimit)
        return (if (status.isNullOrBlank()) query else query.param("status", status))
            .query(::adminPrivacyRequestRecord)
            .list()
            .map(::toPrivacyRequest)
    }

    fun listAuditLogs(familyId: UUID?, action: String?, limit: Int, offset: Int = 0, actorUserId: UUID? = null, actorRole: String? = null, resourceType: String? = null, from: OffsetDateTime? = null, to: OffsetDateTime? = null): List<AdminAuditLogResponse> = listAuditLogsPage(familyId, action, limit, offset, actorUserId, actorRole, resourceType, from, to).items
    fun listAuditLogsPage(familyId: UUID?, action: String?, limit: Int, offset: Int, actorUserId: UUID?, actorRole: String?, resourceType: String?, from: OffsetDateTime?, to: OffsetDateTime?): AdminAuditLogsPage {
        val effectiveLimit = limit
        if (limit !in 1..100) throw BadRequestError("每页条数必须在 1 到 100 之间")
        if (offset < 0) throw BadRequestError("偏移量不能小于 0")
        if (from != null && to != null && from.isAfter(to)) throw BadRequestError("开始时间不能晚于结束时间")
        val clauses = mutableListOf<String>()
        if (familyId != null) clauses += "family_id = :family_id"
        if (!action.isNullOrBlank()) clauses += "action = :action"
        if (actorUserId != null) clauses += "actor_user_id = :actor_user_id"
        if (!actorRole.isNullOrBlank()) clauses += "actor_role = :actor_role"
        if (!resourceType.isNullOrBlank()) clauses += "resource_type = :resource_type"
        if (from != null) clauses += "created_at >= :from_time"
        if (to != null) clauses += "created_at < :to_time"
        val whereClause = if (clauses.isEmpty()) "" else "where ${clauses.joinToString(" and ")}"
        var query = jdbcClient.sql(
            """
            select id, family_id, actor_user_id, actor_role, action, resource_type, resource_id,
                   metadata_json, created_at
            from audit_log
            $whereClause
            order by created_at desc
            limit cast(:limit as integer) offset cast(:offset as integer)
            """.trimIndent(),
        )
            .param("limit", effectiveLimit)
            .param("offset", offset)
        if (familyId != null) query = query.param("family_id", familyId)
        if (!action.isNullOrBlank()) query = query.param("action", action)
        if (actorUserId != null) query = query.param("actor_user_id", actorUserId)
        if (!actorRole.isNullOrBlank()) query = query.param("actor_role", actorRole)
        if (!resourceType.isNullOrBlank()) query = query.param("resource_type", resourceType)
        if (from != null) query = query.param("from_time", from)
        if (to != null) query = query.param("to_time", to)
        val items = query
            .query(adminAuditLogRecord(objectMapper))
            .list()
            .map(::toAuditLog)
        var countQuery = jdbcClient.sql("select count(*) from audit_log $whereClause")
        if (familyId != null) countQuery = countQuery.param("family_id", familyId)
        if (!action.isNullOrBlank()) countQuery = countQuery.param("action", action)
        if (actorUserId != null) countQuery = countQuery.param("actor_user_id", actorUserId)
        if (!actorRole.isNullOrBlank()) countQuery = countQuery.param("actor_role", actorRole)
        if (!resourceType.isNullOrBlank()) countQuery = countQuery.param("resource_type", resourceType)
        if (from != null) countQuery = countQuery.param("from_time", from)
        if (to != null) countQuery = countQuery.param("to_time", to)
        val total = countQuery.query(Long::class.java).single()
        return AdminAuditLogsPage(items, total, offset, limit, offset + items.size < total)
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
              :id, :family_id, 'admin_support', 'media.access_grant_created',
              'media_access_grant', :resource_id, cast(:metadata_json as jsonb)
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
        val grantId = UUID.randomUUID()
        jdbcClient.sql("insert into media_access_grant (id,family_id,media_asset_id,granted_by_actor_role,reason,expires_at,audit_log_id) values (:id,:family_id,:media_asset_id,'admin_support',:reason,:expires_at,:audit_log_id)").param("id", grantId).param("family_id", request.familyId).param("media_asset_id", request.mediaAssetId).param("reason", request.reason.trim().take(500)).param("expires_at", expiresAt).param("audit_log_id", auditLogId).update()
        return AdminMediaAccessGrantResponse(
            id = grantId,
            mediaAssetId = media.id,
            familyId = media.familyId,
            accessUrl = mediaService.createDownloadUrl(media.storageKey),
            expiresAt = expiresAt,
            auditLogId = auditLogId,
            reason = request.reason.trim().take(500), createdAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
        )
    }

    fun listMediaAccessGrants(familyId: UUID?, mediaAssetId: UUID?, active: Boolean?, limit: Int, offset: Int): AdminMediaAccessGrantPage {
        if (limit !in 1..100) throw BadRequestError("每页条数必须在 1 到 100 之间")
        if (offset < 0) throw BadRequestError("偏移量不能小于 0")
        val clauses = mutableListOf<String>()
        if (familyId != null) clauses += "family_id = :family_id"
        if (mediaAssetId != null) clauses += "media_asset_id = :media_asset_id"
        if (active == true) clauses += "revoked_at is null and expires_at > now()"
        if (active == false) clauses += "(revoked_at is not null or expires_at <= now())"
        val where = if (clauses.isEmpty()) "" else "where ${clauses.joinToString(" and ")}"
        var q = jdbcClient.sql("select id,family_id,media_asset_id,granted_by_actor_role,reason,expires_at,revoked_at,revoked_reason,audit_log_id,created_at from media_access_grant $where order by created_at desc,id desc limit cast(:limit as integer) offset cast(:offset as integer)").param("limit", limit).param("offset", offset)
        var c = jdbcClient.sql("select count(*) from media_access_grant $where")
        if (familyId != null) { q = q.param("family_id", familyId); c = c.param("family_id", familyId) }
        if (mediaAssetId != null) { q = q.param("media_asset_id", mediaAssetId); c = c.param("media_asset_id", mediaAssetId) }
        val items = q.query(::adminMediaAccessGrantRecord).list().map(::toMediaGrant)
        val total = c.query(Long::class.java).single()
        return AdminMediaAccessGrantPage(items, total, offset, limit, offset + items.size < total)
    }

    @Transactional
    fun revokeMediaAccessGrant(grantId: UUID, request: AdminMediaAccessGrantRevokeRequest): AdminMediaAccessGrantResponse {
        if (request.reason.isBlank()) throw BadRequestError("撤销原因不能为空。")
        val existing = jdbcClient.sql("select id,family_id,media_asset_id,granted_by_actor_role,reason,expires_at,revoked_at,revoked_reason,audit_log_id,created_at from media_access_grant where id=:id").param("id", grantId).query(::adminMediaAccessGrantRecord).optional().orElseThrow { NotFoundError("媒体授权不存在。") }
        if (existing.revokedAt != null) return toMediaGrant(existing)
        val revokedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
        val reason = request.reason.trim().take(500)
        val auditId = UUID.randomUUID()
        jdbcClient.sql("update media_access_grant set revoked_at=:revoked_at,revoked_reason=:reason where id=:id and revoked_at is null").param("revoked_at", revokedAt).param("reason", reason).param("id", grantId).update()
        jdbcClient.sql("insert into audit_log (id,family_id,actor_role,action,resource_type,resource_id,metadata_json) values (:id,:family,'admin_support','media.access_grant_revoked','media_access_grant',:resource,cast(:metadata as jsonb))").param("id", auditId).param("family", existing.familyId).param("resource", grantId).param("metadata", objectMapper.writeValueAsString(mapOf("reason" to reason))).update()
        return toMediaGrant(existing.copy(revokedAt = revokedAt, revokedReason = reason))
    }

    private fun toMediaGrant(r: AdminMediaAccessGrantRecord) = AdminMediaAccessGrantResponse(r.id, r.mediaAssetId, r.familyId, "", r.expiresAt, r.auditLogId, r.reason, r.revokedAt, r.revokedReason, r.createdAt)

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
