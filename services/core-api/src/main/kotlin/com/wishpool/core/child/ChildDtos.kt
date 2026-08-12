package com.wishpool.core.child

import java.util.UUID

data class CreateChildRequest(
    val nickname: String,
    val birthYear: Int? = null,
    val avatarAsset: String? = null,
    val roomTheme: String? = null,
)

data class UpdateChildRequest(
    val nickname: String? = null,
    val birthYear: Int? = null,
    val avatarAsset: String? = null,
    val roomTheme: String? = null,
)

data class ChildProfileResponse(
    val id: UUID,
    val familyId: UUID,
    val nickname: String,
    val birthYear: Int?,
    val avatarAsset: String?,
    val roomTheme: String,
    val status: String,
)
