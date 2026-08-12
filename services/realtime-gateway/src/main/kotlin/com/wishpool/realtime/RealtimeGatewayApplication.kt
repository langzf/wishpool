package com.wishpool.realtime

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket

fun main() {
    val config = RealtimeConfig.fromEnv()
    val connectionIndex = if (config.redisEnabled) {
        LettuceConnectionIndex(config.redisUri, config.connectionTtlSeconds)
    } else {
        NoopConnectionIndex()
    }
    val coreApiClient = CoreApiClient(config)
    val sessionManager = RealtimeSessionManager(config, coreApiClient, connectionIndex)

    Runtime.getRuntime().addShutdownHook(Thread { connectionIndex.close() })

    embeddedServer(Netty, port = config.port) {
        realtimeGatewayModule(sessionManager)
    }.start(wait = true)
}

fun Application.realtimeGatewayModule(sessionManager: RealtimeSessionManager) {
    install(WebSockets) {
        pingPeriodMillis = 20_000
        timeoutMillis = 30_000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/health") {
            call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
        }

        get("/internal/version") {
            call.respondText("wishpool-realtime-gateway 0.0.1-SNAPSHOT", ContentType.Text.Plain, HttpStatusCode.OK)
        }

        webSocket("/realtime") {
            sessionManager.handleWebSocket(this)
        }

        get("/realtime/sse") {
            sessionManager.handleSse(call)
        }
    }
}
