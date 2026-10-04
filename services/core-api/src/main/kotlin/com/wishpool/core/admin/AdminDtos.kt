package com.wishpool.core.admin

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.validation.constraints.NotBlank

data class AdminDashboardResponse(
    val generatedAt: OffsetDateTime,
    val familyCount: Long,
    val activeChildCount: Long,
    val pendingReviewCount: Long,
    val pendingNotificationCount: Long,
    val pendingOutboxCount: Long,
    val processingMediaCount: Long,
    val runningAiJobCount: Long,
    val openPrivacyRequestCount: Long,
)

data class AdminFamilySummaryResponse(
    val id: UUID,
    val name: String,
    val timezone: String,
    val status: String,
    val childCount: Long,
    val memberCount: Long,
    val createdAt: OffsetDateTime,
)

data class AdminPrivacyRequestResponse(
    val id: UUID,
    val familyId: UUID,
    val requestType: String,
    val status: String,
    val requestedBy: UUID,
    val exportMediaId: UUID?,
    val reason: String?,
    val createdAt: OffsetDateTime,
    val completedAt: OffsetDateTime?,
    val errorMessage: String?,
)

data class AdminAuditLogResponse(
    val id: UUID,
    val familyId: UUID?,
    val actorUserId: UUID?,
    val actorRole: String,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val metadata: JsonNode,
    val createdAt: OffsetDateTime,
)
data class AdminAuditLogsPage(val items: List<AdminAuditLogResponse>, val total: Long, val offset: Int, val limit: Int, val hasMore: Boolean)

data class AdminMediaAccessGrantRequest(
    val familyId: UUID,
    val mediaAssetId: UUID,
    val reason: String,
    val expiresInMinutes: Long = 30,
)

data class AdminMediaAccessGrantResponse(
    val id: UUID? = null,
    val mediaAssetId: UUID,
    val familyId: UUID,
    val accessUrl: String,
    val expiresAt: OffsetDateTime,
    val auditLogId: UUID,
    val reason: String? = null,
    val revokedAt: OffsetDateTime? = null,
    val revokedReason: String? = null,
    val createdAt: OffsetDateTime? = null,
)
data class AdminMediaAccessGrantRevokeRequest(@field:NotBlank(message = "撤销原因不能为空") val reason: String)
data class AdminMediaAccessGrantRecord(val id: UUID, val familyId: UUID, val mediaAssetId: UUID, val actorRole: String, val reason: String, val expiresAt: OffsetDateTime, val revokedAt: OffsetDateTime?, val revokedReason: String?, val auditLogId: UUID, val createdAt: OffsetDateTime)
data class AdminMediaAccessGrantPage(val items: List<AdminMediaAccessGrantResponse>, val total: Long, val offset: Int, val limit: Int, val hasMore: Boolean)

data class AdminFamilySummaryRecord(
    val id: UUID,
    val name: String,
    val timezone: String,
    val status: String,
    val childCount: Long,
    val memberCount: Long,
    val createdAt: OffsetDateTime,
)

data class AdminPrivacyRequestRecord(
    val id: UUID,
    val familyId: UUID,
    val requestType: String,
    val status: String,
    val requestedBy: UUID,
    val exportMediaId: UUID?,
    val reason: String?,
    val createdAt: OffsetDateTime,
    val completedAt: OffsetDateTime?,
    val errorMessage: String?,
)

data class AdminAuditLogRecord(
    val id: UUID,
    val familyId: UUID?,
    val actorUserId: UUID?,
    val actorRole: String,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val metadata: JsonNode,
    val createdAt: OffsetDateTime,
)
