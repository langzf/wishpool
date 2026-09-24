package com.wishpool.notification

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

open class CoreApiClient(
    private val config: NotificationConfig,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build(),
) {
    private val mapper = jacksonObjectMapper()

    open fun claimNotifications(): ClaimNotificationEventsResponse =
        post("internal/notifications/claim", ClaimNotificationEventsRequest(config.claimLimit))

    open fun markDispatchResult(notificationId: UUID, request: NotificationDispatchResultRequest): NotificationEventResponse =
        post("internal/notifications/$notificationId/dispatch-result", request)

    private inline fun <reified T> post(path: String, body: Any): T {
        val response = httpClient.send(
            HttpRequest.newBuilder(resolve(path))
                .timeout(Duration.ofSeconds(30))
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
}

class CoreApiException(
    val statusCode: Int,
    responseBody: String,
) : RuntimeException("Core API request failed with HTTP $statusCode: $responseBody")
