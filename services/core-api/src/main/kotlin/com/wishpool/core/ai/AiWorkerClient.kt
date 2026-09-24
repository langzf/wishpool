package com.wishpool.core.ai

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets

@Component
class AiWorkerClient(
    @Value("\${wishpool.ai.worker.base-url:}") private val aiWorkerBaseUrl: String,
    @Value("\${wishpool.internal.token}") private val internalToken: String,
    private val imageModelProviderService: ImageModelProviderService,
    private val jsonMapper: JsonMapper,
) {
    private val restClient: RestClient = RestClient.builder()
        .requestFactory(JdkClientHttpRequestFactory())
        .configureMessageConverters { converters ->
            converters.disableDefaults()
            converters.addCustomConverter(ByteArrayHttpMessageConverter())
            converters.withJsonConverter(JacksonJsonHttpMessageConverter(jsonMapper))
        }
        .build()

    fun precheckSubmission(request: AiWorkerPrecheckRequest): AiWorkerPrecheckResponse {
        if (aiWorkerBaseUrl.isBlank()) return deterministicPrecheck(request)
        return restClient.post()
            .uri(aiWorkerBaseUrl.trimEnd('/') + "/internal/ai/precheck-submission")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $internalToken")
            .body(jsonBody(request))
            .retrieve()
            .body(AiWorkerPrecheckResponse::class.java)
            ?: deterministicPrecheck(request)
    }

    fun generateWishImage(request: AiWorkerWishImageGenerationRequest, usageCode: String = "wish_card"): AiWorkerWishImageGenerationResponse {
        val provider = imageModelProviderService.resolveProviderRecord(request.provider_code, usageCode)
        val resolvedRequest = request.copy(
            provider_code = provider?.code ?: request.provider_code,
            provider_config = provider?.let(imageModelProviderService::toWorkerConfig),
        )
        if (aiWorkerBaseUrl.isBlank()) return deterministicWishImage(resolvedRequest)
        return restClient.post()
            .uri(aiWorkerBaseUrl.trimEnd('/') + "/internal/ai/generate-wish-image")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $internalToken")
            .body(jsonBody(resolvedRequest))
            .retrieve()
            .body(AiWorkerWishImageGenerationResponse::class.java)
            ?: deterministicWishImage(resolvedRequest)
    }

    private fun deterministicPrecheck(request: AiWorkerPrecheckRequest): AiWorkerPrecheckResponse {
        val mediaKinds = request.media.map { it.kind }.distinct().sorted().ifEmpty { listOf("none") }.joinToString(", ")
        val riskTerms = setOf("危险", "受伤", "陌生人", "隐私", "地址", "电话")
        val joined = buildString {
            append(request.child_note.orEmpty())
            request.media.forEach { media ->
                append(' ')
                append(media.transcript.orEmpty())
                append(' ')
                append(media.visual_labels.joinToString(" "))
            }
        }
        val hasRisk = riskTerms.any { joined.contains(it) }
        return AiWorkerPrecheckResponse(
            submission_id = request.submission_id,
            summary = "${request.task_title} 提交包含 $mediaKinds，已完成基础一致性检查。",
            risk_level = if (hasRisk) "medium" else "low",
            confidence = if (request.media.isEmpty()) 0.54 else 0.72,
            suggested_decision = if (hasRisk) "manual_review" else "approve",
            checklist = listOf("任务标题与提交内容已匹配", "媒体类型已记录", "等待家长确认完成质量"),
            safety_notes = if (hasRisk) listOf("发现潜在风险词，请家长仔细查看") else emptyList(),
        )
    }

    private fun jsonBody(value: Any): ByteArray =
        jsonMapper.writeValueAsString(value).toByteArray(StandardCharsets.UTF_8)

    private fun deterministicWishImage(request: AiWorkerWishImageGenerationRequest): AiWorkerWishImageGenerationResponse =
        AiWorkerWishImageGenerationResponse(
            request_id = request.request_id,
            provider = request.provider_config?.code ?: "deterministic",
            model = request.provider_config?.model_name ?: "deterministic-wish-image",
            prompt = "Create a warm child-safe wish card illustration for: ${request.wish_title}",
            content_type = "image/svg+xml",
            image_base64 = "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDI0IiBoZWlnaHQ9IjEwMjQiPjxyZWN0IHdpZHRoPSIxMDI0IiBoZWlnaHQ9IjEwMjQiIGZpbGw9IiNkYmVhZmUiLz48Y2lyY2xlIGN4PSI1MTIiIGN5PSI1MTIiIHI9IjI0MCIgZmlsbD0iI2ZkZThhOCIvPjwvc3ZnPg==",
            latency_ms = 0,
            cost_units = 0.0,
        )
}
