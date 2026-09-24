package com.wishpool.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.converter.DataConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
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
    val workflowClient = WorkflowClient.newInstance(
        service,
        WorkflowClientOptions.newBuilder()
            .setDataConverter(temporalDataConverter())
            .build(),
    )
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

internal fun temporalDataConverter(): DataConverter {
    val objectMapper = ObjectMapper()
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    return DefaultDataConverter.newDefaultInstance()
        .withPayloadConverterOverrides(JacksonJsonPayloadConverter(objectMapper))
}
