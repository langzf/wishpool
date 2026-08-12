package com.wishpool.core.tasks

import com.wishpool.core.child.ChildProfileResponse
import java.time.LocalDate
import java.util.UUID

data class TaskTemplateResponse(
    val id: UUID,
    val familyId: UUID,
    val title: String,
    val category: String,
    val submissionType: String,
    val description: String?,
    val targetText: String?,
    val defaultDurationSec: Int?,
)

data class CreateTaskTemplateRequest(
    val familyId: UUID,
    val title: String,
    val category: String,
    val submissionType: String,
    val description: String? = null,
    val targetText: String? = null,
    val defaultDurationSec: Int? = null,
)

data class SaveWeeklyPlanRequest(
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val rewardMode: String,
    val wishId: UUID? = null,
    val rules: List<WeeklyPlanRuleInput>,
)

data class WeeklyPlanRuleInput(
    val taskTemplateId: UUID? = null,
    val title: String,
    val category: String,
    val submissionType: String,
    val description: String? = null,
    val targetText: String? = null,
    val weekdays: List<Int>,
    val isCore: Boolean,
    val requireReview: Boolean,
    val sortOrder: Int? = null,
)

data class WeeklyPlanResponse(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val rewardMode: String,
    val status: String,
    val rules: List<WeeklyPlanRuleResponse>,
)

data class WeeklyPlanRuleResponse(
    val id: UUID,
    val taskTemplateId: UUID?,
    val title: String,
    val category: String,
    val submissionType: String,
    val description: String?,
    val targetText: String?,
    val weekdays: List<Int>,
    val isCore: Boolean,
    val requireReview: Boolean,
    val sortOrder: Int,
)

data class TaskInstanceResponse(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val scheduledDate: LocalDate,
    val title: String,
    val category: String,
    val submissionType: String,
    val description: String?,
    val targetText: String?,
    val isCore: Boolean,
    val requireReview: Boolean,
    val status: String,
    val latestSubmissionId: UUID?,
)

data class TodaySnapshotResponse(
    val child: ChildProfileResponse,
    val date: LocalDate,
    val tasks: List<TaskInstanceResponse>,
    val dailySummary: DailySummaryResponse? = null,
    val currentWish: Any? = null,
)

data class DailySummaryResponse(
    val date: LocalDate,
    val coreRequired: Int,
    val coreApproved: Int,
    val coreSkipped: Int,
    val fragmentStatus: String,
)

data class SkipTaskRequest(
    val reason: String,
)

data class PostponeTaskRequest(
    val newDate: LocalDate,
    val reason: String? = null,
)
