package com.wishpool.workflow

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalDataConverterTests {
    @Test
    fun `workflow request DTOs deserialize from Temporal payloads`() {
        val eventId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val converter = temporalDataConverter()

        val requests = listOf(
            MaterializeWeeklyPlanWorkflowRequest(
                weeklyPlanId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
                triggeredByEventId = eventId,
            ),
            EvaluateRewardWorkflowRequest(
                eventType = "review.approved",
                taskInstanceId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                reviewId = null,
                actorUserId = UUID.fromString("44444444-4444-4444-4444-444444444444"),
                triggeredByEventId = eventId,
            ),
            MediaProcessingWorkflowRequest(
                mediaAssetId = UUID.fromString("55555555-5555-5555-5555-555555555555"),
                triggeredByEventId = eventId,
            ),
            AiPrecheckWorkflowRequest(
                submissionId = UUID.fromString("66666666-6666-6666-6666-666666666666"),
                triggeredByEventId = eventId,
            ),
            GenerateMemoryWorkflowRequest(triggeredByEventId = eventId),
            PrivacyDeletionWorkflowRequest(triggeredByEventId = eventId),
        )

        requests.forEach { request ->
            val payload = converter.toPayload(request).orElseThrow()
            val decoded = converter.fromPayload(payload, request.javaClass, request.javaClass)
            assertEquals(request, decoded)
        }
    }
}
