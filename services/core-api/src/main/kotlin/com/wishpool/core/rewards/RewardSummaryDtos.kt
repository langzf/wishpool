package com.wishpool.core.rewards

import java.time.LocalDate
import java.util.UUID

data class RewardSummaryResponse(
    val childId: UUID,
    val weekId: String?,
    val fromDate: LocalDate?,
    val toDate: LocalDate?,
    val starLight: Int,
    val wishFragment: Int,
    val adjustment: Int,
    val total: Int,
)
