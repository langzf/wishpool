package com.wishpool.core.tasks

import com.wishpool.core.shared.nullableInt
import java.sql.ResultSet
import java.sql.Array as SqlArray
import java.util.UUID

fun taskTemplateResponse(rs: ResultSet, rowNum: Int): TaskTemplateResponse =
    TaskTemplateResponse(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        title = rs.getString("title"),
        category = rs.getString("category"),
        submissionType = rs.getString("submission_type"),
        description = rs.getString("description"),
        targetText = rs.getString("target_text"),
        defaultDurationSec = rs.nullableInt("default_duration_sec"),
    )

fun taskInstanceResponse(rs: ResultSet, rowNum: Int): TaskInstanceResponse =
    TaskInstanceResponse(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        scheduledDate = rs.getDate("scheduled_date").toLocalDate(),
        title = rs.getString("title"),
        category = rs.getString("category"),
        submissionType = rs.getString("submission_type"),
        description = rs.getString("description"),
        targetText = rs.getString("target_text"),
        isCore = rs.getBoolean("is_core"),
        requireReview = rs.getBoolean("require_review"),
        status = rs.getString("status"),
        latestSubmissionId = rs.getObject("latest_submission_id", UUID::class.java),
    )

fun weeklyPlanHeader(rs: ResultSet, rowNum: Int): WeeklyPlanHeader =
    WeeklyPlanHeader(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        weekId = rs.getString("week_id"),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date").toLocalDate(),
        rewardMode = rs.getString("reward_mode"),
        status = rs.getString("status"),
    )

fun weeklyPlanRuleResponse(rs: ResultSet, rowNum: Int): WeeklyPlanRuleResponse =
    WeeklyPlanRuleResponse(
        id = rs.getObject("id", UUID::class.java),
        taskTemplateId = rs.getObject("task_template_id", UUID::class.java),
        title = rs.getString("title_snapshot"),
        category = rs.getString("category"),
        submissionType = rs.getString("submission_type"),
        description = rs.getString("description_snapshot"),
        targetText = rs.getString("target_text_snapshot"),
        weekdays = intArray(rs.getArray("weekdays")),
        isCore = rs.getBoolean("is_core"),
        requireReview = rs.getBoolean("require_review"),
        sortOrder = rs.getInt("sort_order"),
    )

private fun intArray(array: SqlArray): List<Int> =
    when (val value = array.array) {
        is Array<*> -> value.map { (it as Number).toInt() }
        is IntArray -> value.toList()
        else -> emptyList()
    }

data class WeeklyPlanHeader(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val rewardMode: String,
    val status: String,
)
