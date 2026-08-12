package com.wishpool.admin

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

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

data class AdminMediaAccessGrantRequest(
    val familyId: UUID,
    val mediaAssetId: UUID,
    val reason: String,
    val expiresInMinutes: Long = 30,
)

data class AdminMediaAccessGrantResponse(
    val mediaAssetId: UUID,
    val familyId: UUID,
    val accessUrl: String,
    val expiresAt: OffsetDateTime,
    val auditLogId: UUID,
)

data class AdminHealthResponse(
    val service: String = "admin-api",
    val status: String,
    val coreApiReachable: Boolean,
)
