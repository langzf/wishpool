package com.wishpool.admin

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID
import kotlin.time.toJavaDuration

class CoreApiClient(
    private val config: AdminConfig,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(config.requestTimeout.inWholeMilliseconds))
        .build(),
) {
    private val mapper = jacksonObjectMapper()

    fun health(): Boolean =
        try {
            val response = httpClient.send(
                HttpRequest.newBuilder(resolve("actuator/health"))
                    .timeout(config.requestTimeout.toJavaDuration())
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.discarding(),
            )
            response.statusCode() in 200..299
        } catch (_: Exception) {
            false
        }

    fun dashboard(): AdminDashboardResponse =
        get("internal/admin/dashboard")

    fun families(limit: Int, status: String?): List<AdminFamilySummaryResponse> =
        get("internal/admin/families?limit=${limit.coerceIn(1, 100)}${status.queryParam("status")}")

    fun privacyRequests(limit: Int, status: String?): List<AdminPrivacyRequestResponse> =
        get("internal/admin/privacy-requests?limit=${limit.coerceIn(1, 100)}${status.queryParam("status")}")

    fun auditLogs(familyId: UUID?, action: String?, limit: Int, offset: Int = 0, actorUserId: UUID? = null, actorRole: String? = null, resourceType: String? = null, from: String? = null, to: String? = null): AdminAuditLogsPage =
        get(
            "internal/admin/audit-logs/paged?limit=${limit.coerceIn(1, 100)}&offset=${offset.coerceAtLeast(0)}" +
                familyId?.let { "&familyId=$it" }.orEmpty() +
                action.queryParam("action") + actorUserId?.let { "&actorUserId=$it" }.orEmpty() + actorRole.queryParam("actorRole") + resourceType.queryParam("resourceType") + from.queryParam("from") + to.queryParam("to"),
        )

    fun grantMediaAccess(request: AdminMediaAccessGrantRequest): AdminMediaAccessGrantResponse =
        post("internal/admin/media-access-grants", request)

    fun listMediaAccessGrants(familyId: UUID?, mediaAssetId: UUID?, active: Boolean?, limit: Int, offset: Int): AdminMediaAccessGrantPage =
        get("internal/admin/media-access-grants?limit=${limit.coerceIn(1, 100)}&offset=${offset.coerceAtLeast(0)}" +
            familyId?.let { "&familyId=$it" }.orEmpty() + mediaAssetId?.let { "&mediaAssetId=$it" }.orEmpty() + active?.let { "&active=$it" }.orEmpty())

    fun revokeMediaAccessGrant(id: UUID, request: AdminMediaAccessGrantRevokeRequest): AdminMediaAccessGrantResponse =
        post("internal/admin/media-access-grants/$id/revoke", request)

    fun imageModelProviders(): List<ImageModelProviderResponse> =
        get("internal/admin/image-model-providers")

    fun createImageModelProvider(request: ImageModelProviderWriteRequest): ImageModelProviderResponse =
        post("internal/admin/image-model-providers", request)

    fun updateImageModelProvider(id: UUID, request: ImageModelProviderWriteRequest): ImageModelProviderResponse =
        put("internal/admin/image-model-providers/$id", request)

    fun toggleImageModelProvider(id: UUID, request: ImageModelProviderToggleRequest): ImageModelProviderResponse =
        post("internal/admin/image-model-providers/$id/toggle", request)

    fun setDefaultImageModelProvider(id: UUID): ImageModelProviderResponse =
        post("internal/admin/image-model-providers/$id/set-default", emptyMap<String, String>())

    fun deleteImageModelProvider(id: UUID) {
        delete("internal/admin/image-model-providers/$id")
    }
    fun aiTextModelProviders(): List<AiTextModelProviderResponse> = get("internal/admin/ai-text-model-providers")
    fun createAiTextModelProvider(r:AiTextModelProviderWriteRequest)=post<AiTextModelProviderResponse>("internal/admin/ai-text-model-providers",r)
    fun updateAiTextModelProvider(id:UUID,r:AiTextModelProviderWriteRequest)=put<AiTextModelProviderResponse>("internal/admin/ai-text-model-providers/$id",r)
    fun toggleAiTextModelProvider(id:UUID,r:AiTextModelProviderToggleRequest)=post<AiTextModelProviderResponse>("internal/admin/ai-text-model-providers/$id/toggle",r)
    fun setDefaultAiTextModelProvider(id:UUID)=post<AiTextModelProviderResponse>("internal/admin/ai-text-model-providers/$id/set-default",emptyMap<String,String>())
    fun testAiTextModelProvider(id:UUID)=post<AiTextProviderTestResponse>("internal/admin/ai-text-model-providers/$id/test-connection",emptyMap<String,String>())
    fun deleteAiTextModelProvider(id:UUID){delete("internal/admin/ai-text-model-providers/$id")}

    fun imageGenUsages(): List<ImageGenUsageResponse> =
        get("internal/admin/image-gen-usages")

    fun upsertImageGenUsage(usageCode: String, request: ImageGenUsageWriteRequest): ImageGenUsageResponse =
        put("internal/admin/image-gen-usages/$usageCode", request.copy(usageCode = usageCode))

    private inline fun <reified T> get(path: String): T {
        val response = httpClient.send(
            HttpRequest.newBuilder(resolve(path))
                .timeout(config.requestTimeout.toJavaDuration())
                .header("X-Internal-Token", config.internalToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        if (response.statusCode() !in 200..299) {
            throw CoreApiException(response.statusCode(), response.body())
        }
        return mapper.readValue(response.body())
    }

    private inline fun <reified T> post(path: String, body: Any): T {
        val response = httpClient.send(
            HttpRequest.newBuilder(resolve(path))
                .timeout(config.requestTimeout.toJavaDuration())
                .header("Content-Type", "application/json")
                .header("X-Internal-Token", config.internalToken)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        if (response.statusCode() !in 200..299) {
            throw CoreApiException(response.statusCode(), response.body())
        }
        return mapper.readValue(response.body())
    }

    private inline fun <reified T> put(path: String, body: Any): T {
        val response = httpClient.send(
            HttpRequest.newBuilder(resolve(path))
                .timeout(config.requestTimeout.toJavaDuration())
                .header("Content-Type", "application/json")
                .header("X-Internal-Token", config.internalToken)
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        if (response.statusCode() !in 200..299) {
            throw CoreApiException(response.statusCode(), response.body())
        }
        return mapper.readValue(response.body())
    }

    private fun delete(path: String) {
        val response = httpClient.send(
            HttpRequest.newBuilder(resolve(path))
                .timeout(config.requestTimeout.toJavaDuration())
                .header("X-Internal-Token", config.internalToken)
                .DELETE()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        if (response.statusCode() !in 200..299) {
            throw CoreApiException(response.statusCode(), response.body())
        }
    }

    private fun resolve(path: String): URI =
        config.coreApiBaseUrl.resolve(path)

    private fun String?.queryParam(name: String): String =
        if (isNullOrBlank()) {
            ""
        } else {
            "&$name=${URLEncoder.encode(this, StandardCharsets.UTF_8)}"
        }
}

class CoreApiException(
    val statusCode: Int,
    responseBody: String,
) : RuntimeException("Core API request failed with HTTP $statusCode: $responseBody")
