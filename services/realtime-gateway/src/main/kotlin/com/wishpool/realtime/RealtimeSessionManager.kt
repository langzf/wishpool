package com.wishpool.realtime

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

class RealtimeSessionManager(
    private val config: RealtimeConfig,
    private val coreApiClient: CoreApiClient,
    private val connectionIndex: ConnectionIndex,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleWebSocket(session: DefaultWebSocketServerSession) {
        val context = try {
            authenticate(session.call, "websocket")
        } catch (ex: RealtimeConnectionRejected) {
            session.send(Frame.Text(errorJson(ex.code, ex.publicMessage)))
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, ex.code))
            return
        }
        val sendMutex = Mutex()
        val emit: suspend (String, Any) -> Unit = { _, payload ->
            sendMutex.withLock {
                session.send(Frame.Text(messageJson(payload)))
            }
        }

        connectionIndex.register(context)
        try {
            emit("realtime.connected", RealtimeConnectedMessage(connectionId = context.connectionId, familyId = context.request.familyId, latestSeq = context.latestSeq))
            coroutineScope {
                val incoming = launch { consumeClientMessages(session, context, emit) }
                val pump = launch {
                    pumpEvents(
                        context = context,
                        emit = emit,
                    )
                }
                incoming.join()
                pump.cancelAndJoin()
            }
        } finally {
            connectionIndex.unregister(context)
        }
    }

    suspend fun handleSse(call: ApplicationCall) {
        val context = try {
            authenticate(call, "sse")
        } catch (ex: RealtimeConnectionRejected) {
            call.respondText(
                text = errorJson(ex.code, ex.publicMessage),
                contentType = ContentType.Application.Json,
                status = ex.status,
            )
            return
        }
        call.response.headers.append(HttpHeaders.CacheControl, "no-cache")
        call.respondTextWriter(contentType = io.ktor.http.ContentType.Text.EventStream) {
            connectionIndex.register(context)
            try {
                writeSse("realtime.connected", RealtimeConnectedMessage(connectionId = context.connectionId, familyId = context.request.familyId, latestSeq = context.latestSeq))
                pumpEvents(
                    context = context,
                    emit = { eventName, payload ->
                        writeSse(eventName, payload)
                    },
                )
            } finally {
                connectionIndex.unregister(context)
            }
        }
    }

    private suspend fun authenticate(call: ApplicationCall, transport: String): RealtimeConnectionContext {
        val token = call.bearerToken()
        val familyId = call.request.queryParameters["familyId"]?.let(::parseUuid)
        val afterSeq = call.request.queryParameters["afterSeq"]?.toLongOrNull() ?: 0L

        if (token == null || familyId == null || afterSeq < 0) {
            throw RealtimeConnectionRejected(
                status = HttpStatusCode.Unauthorized,
                code = "unauthorized",
                publicMessage = "Missing or invalid realtime credentials.",
            )
        }

        return try {
            val me = coreApiClient.me(token)
            val canAccessFamily = me.families.any { it.family.id == familyId && it.member.status == "active" }
            if (!canAccessFamily) {
                throw RealtimeConnectionRejected(
                    status = HttpStatusCode.Forbidden,
                    code = "forbidden",
                    publicMessage = "The authenticated user cannot access this family.",
                )
            }
            RealtimeConnectionContext(
                connectionId = UUID.randomUUID().toString(),
                request = RealtimeConnectionRequest(
                    token = token,
                    familyId = familyId,
                    afterSeq = afterSeq,
                    transport = transport,
                ),
                user = me.user,
                latestSeq = afterSeq,
            )
        } catch (ex: CoreApiException) {
            val status = if (ex.statusCode == 401 || ex.statusCode == 400) HttpStatusCode.Unauthorized else HttpStatusCode.BadGateway
            throw RealtimeConnectionRejected(
                status,
                code = "core_api_error",
                publicMessage = "Core API rejected realtime authentication.",
            )
        }
    }

    private suspend fun consumeClientMessages(
        session: DefaultWebSocketServerSession,
        context: RealtimeConnectionContext,
        emit: suspend (eventName: String, payload: Any) -> Unit,
    ) {
        for (frame in session.incoming) {
            if (frame !is Frame.Text) continue
            val envelope = parseClientEnvelope(frame.readText()) ?: continue
            when (envelope.type) {
                "ping" -> emit("realtime.pong", RealtimePongMessage(sentAt = OffsetDateTime.now()))
                "sync.pull" -> {
                    val requestedSeq = envelope.afterSeq?.takeIf { it >= 0 } ?: context.latestSeq
                    pullAndEmit(context, requestedSeq, emit)
                }
            }
        }
    }

    private suspend fun pumpEvents(
        context: RealtimeConnectionContext,
        emit: suspend (eventName: String, payload: Any) -> Unit,
    ) {
        var elapsedSinceHeartbeat = Duration.ZERO
        while (coroutineContext.isActive) {
            val before = context.latestSeq
            pullAndEmit(context, context.latestSeq, emit)
            if (context.latestSeq == before) {
                elapsedSinceHeartbeat += config.pollInterval
                if (elapsedSinceHeartbeat >= config.heartbeatInterval) {
                    emit(
                        "realtime.heartbeat",
                        RealtimeHeartbeatMessage(
                            connectionId = context.connectionId,
                            latestSeq = context.latestSeq,
                            sentAt = OffsetDateTime.now(),
                        ),
                    )
                    elapsedSinceHeartbeat = Duration.ZERO
                    connectionIndex.refresh(context)
                }
            } else {
                elapsedSinceHeartbeat = Duration.ZERO
            }
            delay(config.pollInterval)
        }
    }

    private suspend fun pullAndEmit(
        context: RealtimeConnectionContext,
        afterSeq: Long,
        emit: suspend (eventName: String, payload: Any) -> Unit,
    ) {
        context.syncMutex.withLock {
            try {
                val response = coreApiClient.pullEvents(
                    accessToken = context.request.token,
                    familyId = context.request.familyId,
                    afterSeq = afterSeq,
                    limit = config.syncLimit,
                )
                for (event in response.events) {
                    emit("family.event", RealtimeFamilyEventMessage(event = event))
                }
                if (response.latestSeq > context.latestSeq) {
                    context.latestSeq = response.latestSeq
                    connectionIndex.refresh(context)
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logger.warn("Realtime pull failed for connection ${context.connectionId}", ex)
                emit(
                    "realtime.error",
                    RealtimeErrorMessage(
                        code = "sync_pull_failed",
                        message = "Unable to pull family events from Core API.",
                    ),
                )
            }
        }
    }

    private fun parseClientEnvelope(text: String): ClientEnvelope? =
        try {
            mapper.readValue<ClientEnvelope>(text)
        } catch (ex: JacksonException) {
            null
        }

    private fun messageJson(payload: Any): String =
        mapper.writeValueAsString(payload)

    private fun errorJson(code: String, message: String): String =
        mapper.writeValueAsString(RealtimeErrorMessage(code = code, message = message))

    private suspend fun java.io.Writer.writeSse(eventName: String, payload: Any) {
        write("event: $eventName\n")
        write("data: ${mapper.writeValueAsString(payload)}\n\n")
        flush()
    }

    private fun parseUuid(value: String): UUID? =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            null
        }
}

private fun ApplicationCall.bearerToken(): String? {
    val headerToken = request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(" ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    return headerToken
        ?: request.queryParameters["accessToken"]?.trim()?.takeIf { it.isNotBlank() }
}

private class RealtimeConnectionRejected(
    val status: HttpStatusCode,
    val code: String,
    val publicMessage: String,
) : RuntimeException(publicMessage)
