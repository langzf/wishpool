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

data class ImageModelProviderResponse(
    val id: UUID,
    val code: String,
    val displayName: String,
    val providerType: String,
    val baseUrl: String,
    val apiKeyMasked: String?,
    val modelName: String,
    val extraParams: JsonNode,
    val isDefault: Boolean,
    val isEnabled: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class ImageModelProviderWriteRequest(
    val code: String? = null,
    val displayName: String,
    val providerType: String,
    val baseUrl: String,
    val apiKey: String? = null,
    val modelName: String,
    val extraParams: JsonNode? = null,
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
)

data class ImageModelProviderToggleRequest(
    val isEnabled: Boolean,
)

data class ImageGenUsageResponse(
    val usageCode: String,
    val providerCode: String,
    val providerDisplayName: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class ImageGenUsageWriteRequest(
    val usageCode: String,
    val providerCode: String,
)

data class AdminHealthResponse(
    val service: String = "admin-api",
    val status: String,
    val coreApiReachable: Boolean,
)
