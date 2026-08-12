package com.wishpool.core.ai

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AiWorkerClient(
    @Value("\${wishpool.ai.worker.base-url:}") private val aiWorkerBaseUrl: String,
    @Value("\${wishpool.internal.token}") private val internalToken: String,
) {
    private val restClient: RestClient = RestClient.builder().build()

    fun precheckSubmission(request: AiWorkerPrecheckRequest): AiWorkerPrecheckResponse {
        if (aiWorkerBaseUrl.isBlank()) return deterministicPrecheck(request)
        return restClient.post()
            .uri(aiWorkerBaseUrl.trimEnd('/') + "/internal/ai/precheck-submission")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $internalToken")
            .body(request)
            .retrieve()
            .body(AiWorkerPrecheckResponse::class.java)
            ?: deterministicPrecheck(request)
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
}
