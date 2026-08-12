package com.wishpool.core.auth

import java.time.OffsetDateTime
import java.util.UUID

data class PhoneCodeRequest(
    val phoneNumber: String,
    val purpose: String,
)

data class PhoneCodeCreated(
    val verificationToken: String,
    val expiresAt: OffsetDateTime,
    val debugCode: String?,
)

data class LoginRequest(
    val provider: String,
    val credential: String,
    val device: DeviceRegistration? = null,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class AuthTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSec: Long,
    val user: UserResponse,
    val primaryFamilyId: UUID?,
)

data class UserResponse(
    val id: UUID,
    val status: String,
    val displayName: String,
    val avatarUrl: String?,
)

data class DeviceRegistration(
    val platform: String,
    val deviceName: String? = null,
    val pushProvider: String? = null,
    val pushToken: String? = null,
)

data class MeResponse(
    val user: UserResponse,
    val families: List<FamilyMemberContextResponse>,
)

data class FamilyMemberContextResponse(
    val family: com.wishpool.core.family.FamilyResponse,
    val member: com.wishpool.core.family.FamilyMemberResponse,
)
