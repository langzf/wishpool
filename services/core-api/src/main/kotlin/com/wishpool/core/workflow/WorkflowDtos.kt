package com.wishpool.core.workflow

import java.time.OffsetDateTime
import java.util.UUID

data class MaterializeWeeklyPlanWorkflowRequest(
    val weeklyPlanId: UUID,
    val triggeredByEventId: UUID? = null,
)

data class EvaluateRewardWorkflowRequest(
    val eventType: String,
    val taskInstanceId: UUID,
    val reviewId: UUID? = null,
    val actorUserId: UUID,
    val triggeredByEventId: UUID? = null,
)

data class RunAiPrecheckWorkflowRequest(
    val submissionId: UUID,
    val triggeredByEventId: UUID? = null,
)

data class GenerateMemoryWorkflowRequest(
    val triggeredByEventId: UUID,
)

data class PrivacyDeletionWorkflowRequest(
    val triggeredByEventId: UUID,
)

data class WorkflowAcceptedResponse(
    val workflow: String,
    val status: String,
    val acceptedAt: OffsetDateTime,
)
