package com.wishpool.core.ai

import com.fasterxml.jackson.annotation.JsonProperty
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
    @JsonProperty("submission_id")
    val submission_id: String,
    @JsonProperty("task_title")
    val task_title: String,
    @JsonProperty("task_category")
    val task_category: String,
    @JsonProperty("child_age")
    val child_age: Int? = null,
    val media: List<AiWorkerMediaSignal>,
    @JsonProperty("child_note")
    val child_note: String? = null,
    @JsonProperty("provider_config") val provider_config: AiWorkerTextProviderConfig? = null,
)

data class AiWorkerTextProviderConfig(val code:String,@JsonProperty("provider_type") val provider_type:String,@JsonProperty("base_url") val base_url:String,@JsonProperty("api_key") val api_key:String?,@JsonProperty("model_name") val model_name:String,val capability:String,@JsonProperty("extra_params") val extra_params: Map<String,Any?> = emptyMap())

data class AiWorkerMediaSignal(
    @JsonProperty("media_id")
    val media_id: String,
    val kind: String,
    @JsonProperty("mime_type")
    val mime_type: String,
    val transcript: String? = null,
    @JsonProperty("visual_labels")
    val visual_labels: List<String> = emptyList(),
    @JsonProperty("duration_seconds")
    val duration_seconds: Double? = null,
    @JsonProperty("image_url") val image_url: String? = null,
)

data class AiWorkerPrecheckResponse(
    @JsonProperty("submission_id")
    val submission_id: String,
    val summary: String,
    @JsonProperty("risk_level")
    val risk_level: String,
    val confidence: Double,
    @JsonProperty("suggested_decision")
    val suggested_decision: String,
    val checklist: List<String>,
    @JsonProperty("safety_notes")
    val safety_notes: List<String>,
    @JsonProperty("provider_status") val provider_status: String = "fallback",
    @JsonProperty("provider_error_code") val provider_error_code: String? = null,
    @JsonProperty("used_provider") val used_provider: String = "deterministic",
    @JsonProperty("used_model") val used_model: String = "deterministic-precheck",
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
