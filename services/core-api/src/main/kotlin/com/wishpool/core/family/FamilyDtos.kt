package com.wishpool.core.family

import com.wishpool.core.child.CreateChildRequest
import java.time.OffsetDateTime
import java.util.UUID

data class CreateFamilyRequest(
    val name: String,
    val timezone: String,
    val firstChild: CreateChildRequest? = null,
)

data class FamilyResponse(
    val id: UUID,
    val name: String,
    val timezone: String,
    val status: String,
)

data class FamilyMemberResponse(
    val id: UUID,
    val familyId: UUID,
    val userId: UUID,
    val role: String,
    val childId: UUID?,
    val displayName: String,
    val status: String,
)

data class InviteParentRequest(
    val contact: String,
)

data class FamilyInviteResponse(
    val id: UUID,
    val familyId: UUID,
    val status: String,
    val expiresAt: OffsetDateTime,
)
