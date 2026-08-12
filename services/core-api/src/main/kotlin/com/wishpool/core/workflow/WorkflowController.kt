package com.wishpool.core.workflow

import com.wishpool.core.ai.AiPrecheckService
import com.wishpool.core.internal.InternalAuthService
import com.wishpool.core.memories.MemoryService
import com.wishpool.core.privacy.PrivacyService
import com.wishpool.core.rewards.RewardService
import com.wishpool.core.tasks.TaskPlanningService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
class WorkflowController(
    private val internalAuthService: InternalAuthService,
    private val taskPlanningService: TaskPlanningService,
    private val rewardService: RewardService,
    private val aiPrecheckService: AiPrecheckService,
    private val memoryService: MemoryService,
    private val privacyService: PrivacyService,
) {
    @PostMapping("/internal/workflows/materialize-weekly-plan")
    fun materializeWeeklyPlan(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: MaterializeWeeklyPlanWorkflowRequest,
    ): Any {
        internalAuthService.requireToken(internalToken)
        return taskPlanningService.materializeWeeklyPlanFromWorkflow(request.weeklyPlanId, request.triggeredByEventId)
    }

    @PostMapping("/internal/workflows/evaluate-reward")
    fun evaluateReward(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: EvaluateRewardWorkflowRequest,
    ): WorkflowAcceptedResponse {
        internalAuthService.requireToken(internalToken)
        rewardService.evaluateWorkflowEvent(
            eventType = request.eventType,
            taskId = request.taskInstanceId,
            reviewId = request.reviewId,
            actorUserId = request.actorUserId,
        )
        return WorkflowAcceptedResponse(
            workflow = "RewardEvaluationWorkflow",
            status = "completed",
            acceptedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )
    }

    @PostMapping("/internal/workflows/run-ai-precheck")
    fun runAiPrecheck(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: RunAiPrecheckWorkflowRequest,
    ): WorkflowAcceptedResponse {
        internalAuthService.requireToken(internalToken)
        aiPrecheckService.runSubmissionPrecheck(
            com.wishpool.core.ai.RunAiPrecheckWorkflowRequest(
                submissionId = request.submissionId,
                triggeredByEventId = request.triggeredByEventId,
            ),
        )
        return WorkflowAcceptedResponse(
            workflow = "AiPrecheckWorkflow",
            status = "completed",
            acceptedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )
    }

    @PostMapping("/internal/workflows/generate-memory")
    fun generateMemory(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: GenerateMemoryWorkflowRequest,
    ): WorkflowAcceptedResponse {
        internalAuthService.requireToken(internalToken)
        memoryService.generateMemoryFromWorkflow(request.triggeredByEventId)
        return WorkflowAcceptedResponse(
            workflow = "GenerateMemoryWorkflow",
            status = "completed",
            acceptedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )
    }

    @PostMapping("/internal/workflows/privacy-deletion")
    fun privacyDeletion(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: PrivacyDeletionWorkflowRequest,
    ): WorkflowAcceptedResponse {
        internalAuthService.requireToken(internalToken)
        privacyService.runDeletionFromWorkflow(request.triggeredByEventId)
        return WorkflowAcceptedResponse(
            workflow = "PrivacyDeletionWorkflow",
            status = "completed",
            acceptedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )
    }
}
