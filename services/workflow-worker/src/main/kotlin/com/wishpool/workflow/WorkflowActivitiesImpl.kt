package com.wishpool.workflow

import java.util.UUID

class WorkflowActivitiesImpl(
    private val coreApiClient: CoreApiClient,
) : WorkflowActivities {
    override fun materializeWeeklyPlan(request: MaterializeWeeklyPlanWorkflowRequest) {
        coreApiClient.materializeWeeklyPlan(request)
    }

    override fun evaluateReward(request: EvaluateRewardWorkflowRequest) {
        coreApiClient.evaluateReward(request)
    }

    override fun generateMemory(triggeredByEventId: UUID) {
        coreApiClient.generateMemory(triggeredByEventId)
    }

    override fun privacyDeletion(triggeredByEventId: UUID) {
        coreApiClient.privacyDeletion(triggeredByEventId)
    }

    override fun markMediaProcessingStarted(request: MediaProcessingWorkflowRequest) {
        coreApiClient.markMediaProcessingStarted(request.mediaAssetId)
    }

    override fun runAiPrecheck(request: AiPrecheckWorkflowRequest) {
        coreApiClient.runAiPrecheck(request)
    }
}
