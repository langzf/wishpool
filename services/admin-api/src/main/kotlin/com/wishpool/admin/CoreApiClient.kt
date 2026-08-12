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

    fun auditLogs(familyId: UUID?, action: String?, limit: Int): List<AdminAuditLogResponse> =
        get(
            "internal/admin/audit-logs?limit=${limit.coerceIn(1, 100)}" +
                familyId?.let { "&familyId=$it" }.orEmpty() +
                action.queryParam("action"),
        )

    fun grantMediaAccess(request: AdminMediaAccessGrantRequest): AdminMediaAccessGrantResponse =
        post("internal/admin/media-access-grants", request)

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
