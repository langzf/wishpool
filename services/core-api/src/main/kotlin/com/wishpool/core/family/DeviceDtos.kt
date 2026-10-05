package com.wishpool.core.family

import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class FamilyDeviceResponse(
    val id: UUID, val platform: String, val deviceName: String?, val childId: UUID?,
    val pushProvider: String?, val pushTokenRegistered: Boolean,
    val lastSeenAt: OffsetDateTime?, val createdAt: OffsetDateTime, val revokedAt: OffsetDateTime?
)
data class RevokeDeviceRequest(@field:Size(max = 500) val reason: String? = null)
