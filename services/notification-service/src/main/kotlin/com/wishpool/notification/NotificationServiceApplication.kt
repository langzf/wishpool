package com.wishpool.notification

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.concurrent.Executors

fun main() {
    val config = NotificationConfig.fromEnv()
    val coreApiClient = CoreApiClient(config)
    val dispatcher = NotificationDispatcher(config, coreApiClient)
    val executor = Executors.newSingleThreadScheduledExecutor()
    executor.scheduleWithFixedDelay(
        { runCatching { dispatcher.dispatchOnce() } },
        0,
        config.pollInterval.inWholeMilliseconds,
        java.util.concurrent.TimeUnit.MILLISECONDS,
    )
    embeddedServer(Netty, port = config.port) {
        notificationModule(NotificationRuntime(config, dispatcher))
    }.start(wait = true)
}

data class NotificationRuntime(
    val config: NotificationConfig,
    val dispatcher: NotificationDispatcher,
)

fun Application.notificationModule(runtime: NotificationRuntime) {
    val mapper = jacksonObjectMapper()
    routing {
        get("/health") {
            call.respondText(
                mapper.writeValueAsString(
                    NotificationHealthResponse(
                        status = "ready",
                        dispatchEnabled = runtime.config.dispatchEnabled,
                    ),
                ),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
        post("/internal/notifications/dispatch-once") {
            if (!call.requireInternalToken(runtime.config.internalToken)) return@post
            val dispatched = runtime.dispatcher.dispatchOnce()
            call.respondText(
                mapper.writeValueAsString(mapOf("dispatched" to dispatched)),
                contentType = ContentType.Application.Json,
            )
        }
    }
}

suspend fun io.ktor.server.application.ApplicationCall.requireInternalToken(expected: String): Boolean {
    if (request.headers["X-Internal-Token"] != expected) {
        respondText(
            """{"detail":"Internal token is required."}""",
            contentType = ContentType.Application.Json,
            status = HttpStatusCode.Unauthorized,
        )
        return false
    }
    return true
}
