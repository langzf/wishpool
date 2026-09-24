package com.wishpool.core.ai

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class ImageModelProviderResponse(
    val id: UUID,
    val code: String,
    val displayName: String,
    val providerType: String,
    val baseUrl: String,
    val apiKeyMasked: String?,
    val modelName: String,
    val extraParams: JsonNode,
    val isDefault: Boolean,
    val isEnabled: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class ImageModelProviderWriteRequest(
    val code: String? = null,
    val displayName: String,
    val providerType: String,
    val baseUrl: String,
    val apiKey: String? = null,
    val modelName: String,
    val extraParams: JsonNode? = null,
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
)

data class ImageModelProviderToggleRequest(
    val isEnabled: Boolean,
)

data class ImageGenUsageResponse(
    val usageCode: String,
    val providerCode: String,
    val providerDisplayName: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class ImageGenUsageWriteRequest(
    val usageCode: String,
    val providerCode: String,
)

data class BusinessImageModelProviderListResponse(
    val usageCode: String,
    val selectedProviderCode: String?,
    val providers: List<ImageModelProviderResponse>,
)

data class AiWorkerImageProviderConfig(
    val code: String,
    @JsonProperty("provider_type")
    val provider_type: String,
    @JsonProperty("base_url")
    val base_url: String,
    @JsonProperty("api_key")
    val api_key: String?,
    @JsonProperty("model_name")
    val model_name: String,
    @JsonProperty("extra_params")
    val extra_params: Map<String, Any?>,
)

data class AiWorkerWishImageGenerationRequest(
    @JsonProperty("request_id")
    val request_id: String,
    @JsonProperty("family_id")
    val family_id: String,
    @JsonProperty("child_age")
    val child_age: Int? = null,
    @JsonProperty("wish_title")
    val wish_title: String,
    @JsonProperty("wish_note")
    val wish_note: String? = null,
    val category: String? = null,
    val style: String = "warm_illustration",
    @JsonProperty("aspect_ratio")
    val aspect_ratio: String = "1:1",
    @JsonProperty("negative_prompt")
    val negative_prompt: String? = null,
    @JsonProperty("provider_code")
    val provider_code: String? = null,
    @JsonProperty("provider_config")
    val provider_config: AiWorkerImageProviderConfig? = null,
)

data class AiWorkerWishImageGenerationResponse(
    @JsonProperty("request_id")
    val request_id: String,
    val provider: String,
    val model: String,
    val prompt: String,
    @JsonProperty("content_type")
    val content_type: String,
    @JsonProperty("image_base64")
    val image_base64: String? = null,
    @JsonProperty("image_url")
    val image_url: String? = null,
    @JsonProperty("latency_ms")
    val latency_ms: Long? = null,
    @JsonProperty("cost_units")
    val cost_units: Double? = null,
)

data class ImageModelProviderRecord(
    val id: UUID,
    val code: String,
    val displayName: String,
    val providerType: String,
    val baseUrl: String,
    val apiKey: String?,
    val modelName: String,
    val extraParams: JsonNode,
    val isDefault: Boolean,
    val isEnabled: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class ImageGenUsageRecord(
    val usageCode: String,
    val providerCode: String,
    val providerDisplayName: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
