package com.wishpool.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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
        get("/admin/media-access-grants") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@get
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.listMediaAccessGrants(
                familyId = call.request.queryParameters["familyId"]?.let(UUID::fromString),
                mediaAssetId = call.request.queryParameters["mediaAssetId"]?.let(UUID::fromString),
                active = call.request.queryParameters["active"]?.toBooleanStrictOrNull(),
                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0,
            )))
        }
        post("/admin/media-access-grants/{grantId}/revoke") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@post
            val grantId = call.parameters["grantId"]?.let(UUID::fromString)
                ?: return@post call.respondJson("""{"detail":"授权 ID 无效。"}""", HttpStatusCode.BadRequest)
            val request = try { mapper.readValue<AdminMediaAccessGrantRevokeRequest>(call.receiveText()) }
            catch (_: Exception) { return@post call.respondJson("""{"detail":"撤销原因不能为空"}""", HttpStatusCode.BadRequest) }
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.revokeMediaAccessGrant(grantId, request)))
        }
        get("/admin/image-model-providers") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@get
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.imageModelProviders()))
        }
        post("/admin/image-model-providers") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@post
            val request = mapper.readValue<ImageModelProviderWriteRequest>(call.receiveText())
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.createImageModelProvider(request)))
        }
        put("/admin/image-model-providers/{id}") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@put
            val id = call.parameters["id"]?.let(UUID::fromString)
                ?: return@put call.respondJson("""{"detail":"Provider id is required."}""", HttpStatusCode.BadRequest)
            val request = mapper.readValue<ImageModelProviderWriteRequest>(call.receiveText())
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.updateImageModelProvider(id, request)))
        }
        post("/admin/image-model-providers/{id}/toggle") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@post
            val id = call.parameters["id"]?.let(UUID::fromString)
                ?: return@post call.respondJson("""{"detail":"Provider id is required."}""", HttpStatusCode.BadRequest)
            val request = mapper.readValue<ImageModelProviderToggleRequest>(call.receiveText())
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.toggleImageModelProvider(id, request)))
        }
        post("/admin/image-model-providers/{id}/set-default") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@post
            val id = call.parameters["id"]?.let(UUID::fromString)
                ?: return@post call.respondJson("""{"detail":"Provider id is required."}""", HttpStatusCode.BadRequest)
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.setDefaultImageModelProvider(id)))
        }
        delete("/admin/image-model-providers/{id}") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@delete
            val id = call.parameters["id"]?.let(UUID::fromString)
                ?: return@delete call.respondJson("""{"detail":"Provider id is required."}""", HttpStatusCode.BadRequest)
            runtime.coreApiClient.deleteImageModelProvider(id)
            call.respondJson("""{"status":"deleted"}""")
        }
        get("/admin/ai-text-model-providers") { if (!call.requireAdminToken(runtime.config.adminToken)) return@get; call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.aiTextModelProviders())) }
        post("/admin/ai-text-model-providers") { if (!call.requireAdminToken(runtime.config.adminToken)) return@post; call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.createAiTextModelProvider(mapper.readValue(call.receiveText())))) }
        put("/admin/ai-text-model-providers/{id}") { if (!call.requireAdminToken(runtime.config.adminToken)) return@put; val id=UUID.fromString(call.parameters["id"]); call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.updateAiTextModelProvider(id,mapper.readValue(call.receiveText())))) }
        post("/admin/ai-text-model-providers/{id}/toggle") { if (!call.requireAdminToken(runtime.config.adminToken)) return@post; val id=UUID.fromString(call.parameters["id"]); call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.toggleAiTextModelProvider(id,mapper.readValue(call.receiveText())))) }
        post("/admin/ai-text-model-providers/{id}/set-default") { if (!call.requireAdminToken(runtime.config.adminToken)) return@post; call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.setDefaultAiTextModelProvider(UUID.fromString(call.parameters["id"])))) }
        post("/admin/ai-text-model-providers/{id}/test-connection") { if (!call.requireAdminToken(runtime.config.adminToken)) return@post; call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.testAiTextModelProvider(UUID.fromString(call.parameters["id"])))) }
        delete("/admin/ai-text-model-providers/{id}") { if (!call.requireAdminToken(runtime.config.adminToken)) return@delete; runtime.coreApiClient.deleteAiTextModelProvider(UUID.fromString(call.parameters["id"])); call.respondJson("{\"status\":\"deleted\"}") }
        get("/admin/image-gen-usages") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@get
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.imageGenUsages()))
        }
        put("/admin/image-gen-usages/{usageCode}") {
            if (!call.requireAdminToken(runtime.config.adminToken)) return@put
            val usageCode = call.parameters["usageCode"]
                ?: return@put call.respondJson("""{"detail":"Usage code is required."}""", HttpStatusCode.BadRequest)
            val request = mapper.readValue<ImageGenUsageWriteRequest>(call.receiveText())
            call.respondJson(mapper.writeValueAsString(runtime.coreApiClient.upsertImageGenUsage(usageCode, request)))
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
