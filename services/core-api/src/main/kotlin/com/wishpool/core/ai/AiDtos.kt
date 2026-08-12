package com.wishpool.core.ai

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class AiPrecheckResponse(
    val id: UUID,
    val submissionId: UUID,
    val type: String,
    val summary: String,
    val confidence: Double?,
    val flags: JsonNode,
    val model: Map<String, Any?>,
)

data class RunAiPrecheckWorkflowRequest(
    val submissionId: UUID,
    val triggeredByEventId: UUID? = null,
)

data class AiWorkerPrecheckRequest(
    val submission_id: String,
    val task_title: String,
    val task_category: String,
    val child_age: Int? = null,
    val media: List<AiWorkerMediaSignal>,
    val child_note: String? = null,
)

data class AiWorkerMediaSignal(
    val media_id: String,
    val kind: String,
    val mime_type: String,
    val transcript: String? = null,
    val visual_labels: List<String> = emptyList(),
    val duration_seconds: Double? = null,
)

data class AiWorkerPrecheckResponse(
    val submission_id: String,
    val summary: String,
    val risk_level: String,
    val confidence: Double,
    val suggested_decision: String,
    val checklist: List<String>,
    val safety_notes: List<String>,
)

data class AiJobRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val submissionId: UUID,
    val jobType: String,
    val status: String,
    val modelRoute: String?,
    val attemptCount: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class AiPrecheckRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val aiJobId: UUID,
    val submissionId: UUID,
    val type: String,
    val summary: String,
    val confidence: Double?,
    val flags: JsonNode,
    val modelProvider: String,
    val modelName: String,
    val modelVersion: String,
    val promptVersion: String?,
    val createdAt: OffsetDateTime,
)
