package com.wishpool.realtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

class CoreApiClient(
    private val config: RealtimeConfig,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(config.httpTimeoutSeconds))
        .build(),
) {
    private val mapper = jacksonObjectMapper()

    suspend fun me(accessToken: String): MeResponse =
        get("/me", accessToken)

    suspend fun pullEvents(
        accessToken: String,
        familyId: UUID,
        afterSeq: Long,
        limit: Int = config.syncLimit,
    ): SyncPullResponse =
        get(
            path = "/sync/pull?familyId=${familyId.url()}&afterSeq=$afterSeq&limit=$limit",
            accessToken = accessToken,
        )

    private suspend inline fun <reified T> get(path: String, accessToken: String): T =
        withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder(resolve(path))
                .timeout(Duration.ofSeconds(config.httpTimeoutSeconds))
                .header("Authorization", "Bearer $accessToken")
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw CoreApiException(response.statusCode(), response.body())
            }
            mapper.readValue(response.body())
        }

    private fun resolve(path: String): URI =
        config.coreApiBaseUrl.resolve(path.trimStart('/'))

    private fun UUID.url(): String =
        URLEncoder.encode(toString(), StandardCharsets.UTF_8)
}

class CoreApiException(
    val statusCode: Int,
    responseBody: String,
) : RuntimeException("Core API request failed with HTTP $statusCode: $responseBody")
