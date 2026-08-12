package com.wishpool.workflow

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class ClaimOutboxEventsRequest(
    val limit: Int,
    val leaseSeconds: Long,
)

data class RetryOutboxEventRequest(
    val delaySeconds: Long,
    val reason: String? = null,
)

data class OutboxClaimResponse(
    val events: List<OutboxEvent>,
)

data class OutboxEvent(
    val id: UUID,
    val type: String,
    val aggregateType: String,
    val aggregateId: UUID,
    val payload: JsonNode,
    val availableAt: OffsetDateTime,
    val leasedUntil: OffsetDateTime?,
    val retryCount: Int,
    val createdAt: OffsetDateTime,
)

data class WorkflowAcceptedResponse(
    val workflow: String,
    val status: String,
    val acceptedAt: OffsetDateTime,
)

data class MaterializeWeeklyPlanWorkflowRequest(
    val weeklyPlanId: UUID,
    val triggeredByEventId: UUID,
)

data class EvaluateRewardWorkflowRequest(
    val eventType: String,
    val taskInstanceId: UUID,
    val reviewId: UUID? = null,
    val actorUserId: UUID,
    val triggeredByEventId: UUID,
)

data class MediaProcessingWorkflowRequest(
    val mediaAssetId: UUID,
    val triggeredByEventId: UUID,
)

data class AiPrecheckWorkflowRequest(
    val submissionId: UUID,
    val triggeredByEventId: UUID,
)

data class GenerateMemoryWorkflowRequest(
    val triggeredByEventId: UUID,
)

data class PrivacyDeletionWorkflowRequest(
    val triggeredByEventId: UUID,
)
