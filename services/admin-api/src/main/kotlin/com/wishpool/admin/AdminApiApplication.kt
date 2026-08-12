package com.wishpool.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

fun main() {
    val config = AdminConfig.fromEnv()
    embeddedServer(Netty, port = config.port) {
        adminModule(AdminRuntime(config, CoreApiClient(config)))
    }.start(wait = true)
}

data class AdminRuntime(
    val config: AdminConfig,
    val coreApiClient: CoreApiClient,
)

fun Application.adminModule(runtime: AdminRuntime) {
    val mapper = jacksonObjectMapper()
    routing {
        get("/health") {
            val coreReachable = runtime.coreApiClient.health()
            val body = AdminHealthResponse(status = if (coreReachable) "ready" else "degraded", coreApiReachable = coreReachable)
            call.respondJson(mapper.writeValueAsString(body), if (coreReachable) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable)
        }
        get("/admin/dashboard") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@get
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.dashboard()))
        }
        get("/admin/families") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@get
            call.respondJson(
                mapper.writeValueAsString(
                    runtime.coreApiClient.families(
                        limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                        status = call.request.queryParameters["status"],
                    ),
                ),
            )
        }
        get("/admin/privacy-requests") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@get
            call.respondJson(
                mapper.writeValueAsString(
                    runtime.coreApiClient.privacyRequests(
                        limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                        status = call.request.queryParameters["status"],
                    ),
                ),
            )
        }
        get("/admin/audit-logs") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@get
            call.respondJson(
                mapper.writeValueAsString(
                    runtime.coreApiClient.auditLogs(
                        familyId = call.request.queryParameters["familyId"]?.let(UUID::fromString),
                        action = call.request.queryParameters["action"],
                        limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                    ),
                ),
            )
        }
        post("/admin/media-access-grants") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@post
            val request = mapper.readValue<AdminMediaAccessGrantRequest>(call.receiveText())
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.grantMediaAccess(request)))
        }
    }
}

suspend fun io.ktor.server.application.ApplicationCall.respondJson(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondText(body, status = status, contentType = io.ktor.http.ContentType.Application.Json)
}

suspend fun io.ktor.server.application.ApplicationCall.requireAdminToken(expected: String): Boolean {
    if (request.headers["X-Admin-Token"] != expected) {
        respondJson("""{"detail":"Admin token is required."}""", HttpStatusCode.Unauthorized)
        return false
    }
    return true
}
