package com.wishpool.workflow

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import java.time.Duration
import java.util.UUID

@WorkflowInterface
interface MaterializeWeeklyPlanWorkflow {
    @WorkflowMethod
    fun run(request: MaterializeWeeklyPlanWorkflowRequest)
}

class MaterializeWeeklyPlanWorkflowImpl : MaterializeWeeklyPlanWorkflow {
    private val activities = workflowActivities()

    override fun run(request: MaterializeWeeklyPlanWorkflowRequest) {
        activities.materializeWeeklyPlan(request)
    }
}

@WorkflowInterface
interface RewardEvaluationWorkflow {
    @WorkflowMethod
    fun run(request: EvaluateRewardWorkflowRequest)
}

class RewardEvaluationWorkflowImpl : RewardEvaluationWorkflow {
    private val activities = workflowActivities()

    override fun run(request: EvaluateRewardWorkflowRequest) {
        activities.evaluateReward(request)
    }
}

@WorkflowInterface
interface GenerateMemoryWorkflow {
    @WorkflowMethod
    fun run(triggeredByEventId: UUID)
}

class GenerateMemoryWorkflowImpl : GenerateMemoryWorkflow {
    private val activities = workflowActivities()

    override fun run(triggeredByEventId: UUID) {
        activities.generateMemory(triggeredByEventId)
    }
}

@WorkflowInterface
interface PrivacyDeletionWorkflow {
    @WorkflowMethod
    fun run(triggeredByEventId: UUID)
}

class PrivacyDeletionWorkflowImpl : PrivacyDeletionWorkflow {
    private val activities = workflowActivities()

    override fun run(triggeredByEventId: UUID) {
        activities.privacyDeletion(triggeredByEventId)
    }
}

@WorkflowInterface
interface MediaProcessingWorkflow {
    @WorkflowMethod
    fun run(request: MediaProcessingWorkflowRequest)
}

class MediaProcessingWorkflowImpl : MediaProcessingWorkflow {
    private val activities = workflowActivities()

    override fun run(request: MediaProcessingWorkflowRequest) {
        activities.markMediaProcessingStarted(request)
    }
}

@WorkflowInterface
interface AiPrecheckWorkflow {
    @WorkflowMethod
    fun run(request: AiPrecheckWorkflowRequest)
}

class AiPrecheckWorkflowImpl : AiPrecheckWorkflow {
    private val activities = workflowActivities()

    override fun run(request: AiPrecheckWorkflowRequest) {
        activities.runAiPrecheck(request)
    }
}

@ActivityInterface
interface WorkflowActivities {
    fun materializeWeeklyPlan(request: MaterializeWeeklyPlanWorkflowRequest)
    fun evaluateReward(request: EvaluateRewardWorkflowRequest)
    fun generateMemory(triggeredByEventId: UUID)
    fun privacyDeletion(triggeredByEventId: UUID)
    fun markMediaProcessingStarted(request: MediaProcessingWorkflowRequest)
    fun runAiPrecheck(request: AiPrecheckWorkflowRequest)
}

private fun workflowActivities(): WorkflowActivities =
    Workflow.newActivityStub(
        WorkflowActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .build(),
    )
