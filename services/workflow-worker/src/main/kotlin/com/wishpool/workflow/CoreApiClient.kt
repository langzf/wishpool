package com.wishpool.workflow

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

class CoreApiClient(
    private val config: WorkerConfig,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build(),
) {
    private val mapper = jacksonObjectMapper()

    fun claimOutboxEvents(): OutboxClaimResponse =
        post(
            path = "/internal/outbox/events/claim",
            body = ClaimOutboxEventsRequest(
                limit = config.outboxClaimLimit,
                leaseSeconds = config.outboxPollInterval.seconds.coerceAtLeast(30),
            ),
        )

    fun markPublished(eventId: UUID) {
        post<Unit>(
            path = "/internal/outbox/events/$eventId/published",
            body = emptyMap<String, String>(),
        )
    }

    fun scheduleRetry(eventId: UUID, reason: String?) {
        post<OutboxEvent>(
            path = "/internal/outbox/events/$eventId/retry",
            body = RetryOutboxEventRequest(
                delaySeconds = config.outboxRetryDelaySeconds,
                reason = reason?.take(2000),
            ),
        )
    }

    fun materializeWeeklyPlan(request: MaterializeWeeklyPlanWorkflowRequest) {
        post<Unit>("/internal/workflows/materialize-weekly-plan", request)
    }

    fun evaluateReward(request: EvaluateRewardWorkflowRequest): WorkflowAcceptedResponse =
        post("/internal/workflows/evaluate-reward", request)

    fun generateMemory(triggeredByEventId: UUID): WorkflowAcceptedResponse =
        post("/internal/workflows/generate-memory", GenerateMemoryWorkflowRequest(triggeredByEventId))

    fun privacyDeletion(triggeredByEventId: UUID): WorkflowAcceptedResponse =
        post("/internal/workflows/privacy-deletion", PrivacyDeletionWorkflowRequest(triggeredByEventId))

    fun markMediaProcessingStarted(mediaAssetId: UUID) {
        post<Unit>("/internal/media/$mediaAssetId/processing-started", emptyMap<String, String>())
    }

    fun runAiPrecheck(request: AiPrecheckWorkflowRequest): WorkflowAcceptedResponse =
        post("/internal/workflows/run-ai-precheck", request)

    private inline fun <reified T> post(path: String, body: Any): T {
        val request = HttpRequest.newBuilder(resolve(path))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("X-Internal-Token", config.internalToken)
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw CoreApiException(response.statusCode(), response.body())
        }
        if (T::class == Unit::class) {
            @Suppress("UNCHECKED_CAST")
            return Unit as T
        }
        return mapper.readValue(response.body())
    }

    private fun resolve(path: String): URI =
        config.coreApiBaseUrl.resolve(path)
}

class CoreApiException(
    val statusCode: Int,
    responseBody: String,
) : RuntimeException("Core API request failed with HTTP $statusCode: $responseBody")
