package com.wishpool.workflow

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowExecutionAlreadyStarted
import io.temporal.client.WorkflowOptions
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class OutboxPublisher(
    private val config: WorkerConfig,
    private val coreApiClient: CoreApiClient,
    private val workflowClient: WorkflowClient,
) {
    private val running = AtomicBoolean(false)

    fun start() {
        if (!running.compareAndSet(false, true)) return
        while (running.get()) {
            runOnce()
            Thread.sleep(config.outboxPollInterval.toMillis())
        }
    }

    fun stop() {
        running.set(false)
    }

    fun runOnce() {
        val events = coreApiClient.claimOutboxEvents().events
        events.forEach(::publish)
    }

    private fun publish(event: OutboxEvent) {
        try {
            when (event.type) {
                "planning.weekly_plan_saved" -> startMaterializeWeeklyPlan(event)
                "review.approved", "task.skipped" -> startRewardEvaluation(event)
                "review.revoked" -> {
                    if (event.payload.path("decision").asString() == "approved") {
                        startRewardEvaluation(event)
                    }
                }
                "wish.redeemed" -> startGenerateMemory(event)
                "privacy.deletion_requested" -> startPrivacyDeletion(event)
                "media.uploaded" -> startMediaProcessing(event)
                "submission.created" -> startAiPrecheck(event)
                else -> logger.debug("No workflow route for outbox event type {}", event.type)
            }
            coreApiClient.markPublished(event.id)
        } catch (ex: WorkflowExecutionAlreadyStarted) {
            logger.info("Workflow for outbox event {} already exists; marking event as published.", event.id)
            coreApiClient.markPublished(event.id)
        } catch (ex: Exception) {
            logger.warn("Failed to publish outbox event {}", event.id, ex)
            coreApiClient.scheduleRetry(event.id, ex.message)
        }
    }

    private fun startMaterializeWeeklyPlan(event: OutboxEvent) {
        val weeklyPlanId = event.aggregateId
        val workflow = workflowClient.newWorkflowStub(
            MaterializeWeeklyPlanWorkflow::class.java,
            workflowOptions("materialize-weekly-plan", event.id),
        )
        WorkflowClient.start(
            workflow::run,
            MaterializeWeeklyPlanWorkflowRequest(
                weeklyPlanId = weeklyPlanId,
                triggeredByEventId = event.id,
            ),
        )
    }

    private fun startRewardEvaluation(event: OutboxEvent) {
        val taskInstanceId = event.payload.path("taskInstanceId").requiredUuid("taskInstanceId")
        val reviewId = event.payload.path("reviewId").optionalUuid()
        val actorUserId = event.payload.path("actorUserId")
            .optionalUuid()
            ?: event.payload.path("reviewedBy").optionalUuid()
            ?: event.payload.path("pairedUserId").optionalUuid()
            ?: event.payload.path("createdBy").optionalUuid()
            ?: ZERO_UUID
        val workflow = workflowClient.newWorkflowStub(
            RewardEvaluationWorkflow::class.java,
            workflowOptions("reward-evaluation", event.id),
        )
        WorkflowClient.start(
            workflow::run,
            EvaluateRewardWorkflowRequest(
                eventType = event.type,
                taskInstanceId = taskInstanceId,
                reviewId = reviewId,
                actorUserId = actorUserId,
                triggeredByEventId = event.id,
            ),
        )
    }

    private fun startGenerateMemory(event: OutboxEvent) {
        val workflow = workflowClient.newWorkflowStub(
            GenerateMemoryWorkflow::class.java,
            workflowOptions("generate-memory", event.id),
        )
        WorkflowClient.start(workflow::run, event.id)
    }

    private fun startPrivacyDeletion(event: OutboxEvent) {
        val workflow = workflowClient.newWorkflowStub(
            PrivacyDeletionWorkflow::class.java,
            workflowOptions("privacy-deletion", event.id),
        )
        WorkflowClient.start(workflow::run, event.id)
    }

    private fun startMediaProcessing(event: OutboxEvent) {
        val mediaAssetId = event.payload.path("mediaAssetId")
            .optionalUuid()
            ?: event.aggregateId
        val workflow = workflowClient.newWorkflowStub(
            MediaProcessingWorkflow::class.java,
            workflowOptions("media-processing", event.id),
        )
        WorkflowClient.start(
            workflow::run,
            MediaProcessingWorkflowRequest(
                mediaAssetId = mediaAssetId,
                triggeredByEventId = event.id,
            ),
        )
    }

    private fun startAiPrecheck(event: OutboxEvent) {
        val submissionId = event.payload.path("submissionId")
            .optionalUuid()
            ?: event.aggregateId
        val workflow = workflowClient.newWorkflowStub(
            AiPrecheckWorkflow::class.java,
            workflowOptions("ai-precheck", event.id),
        )
        WorkflowClient.start(
            workflow::run,
            AiPrecheckWorkflowRequest(
                submissionId = submissionId,
                triggeredByEventId = event.id,
            ),
        )
    }

    private fun workflowOptions(prefix: String, eventId: UUID): WorkflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(config.temporalTaskQueue)
            .setWorkflowId("wishpool-$prefix-$eventId")
            .setWorkflowRunTimeout(Duration.ofMinutes(30))
            .build()

    private companion object {
        val ZERO_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val logger = LoggerFactory.getLogger(OutboxPublisher::class.java)
    }
}
