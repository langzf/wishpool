package com.wishpool.workflow

import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import org.slf4j.LoggerFactory

fun main() {
    val config = WorkerConfig.fromEnv()
    val coreApiClient = CoreApiClient(config)
    val service = WorkflowServiceStubs.newServiceStubs(
        WorkflowServiceStubsOptions.newBuilder()
            .setTarget(config.temporalTarget)
            .build(),
    )
    val workflowClient = WorkflowClient.newInstance(service)
    val factory = WorkerFactory.newInstance(workflowClient)
    val worker = factory.newWorker(config.temporalTaskQueue)

    worker.registerWorkflowImplementationTypes(
        MaterializeWeeklyPlanWorkflowImpl::class.java,
        RewardEvaluationWorkflowImpl::class.java,
        GenerateMemoryWorkflowImpl::class.java,
        PrivacyDeletionWorkflowImpl::class.java,
        MediaProcessingWorkflowImpl::class.java,
        AiPrecheckWorkflowImpl::class.java,
    )
    worker.registerActivitiesImplementations(WorkflowActivitiesImpl(coreApiClient))
    factory.start()

    val publisher = OutboxPublisher(config, coreApiClient, workflowClient)
    Runtime.getRuntime().addShutdownHook(
        Thread {
            publisher.stop()
            factory.shutdown()
            service.shutdown()
        },
    )
    logger.info("WishPool workflow worker started on task queue {}", config.temporalTaskQueue)
    publisher.start()
}

private val logger = LoggerFactory.getLogger("com.wishpool.workflow.WorkflowWorkerApplication")
