package com.wishpool.notification

import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ProviderResult(val status: String, val providerMessageId: String? = null, val errorMessage: String? = null)
interface PushAdapter { fun send(device: NotificationDeviceResponse, notification: NotificationEventResponse): ProviderResult }
class NonePushAdapter : PushAdapter { override fun send(device: NotificationDeviceResponse, notification: NotificationEventResponse) = ProviderResult("failed", errorMessage = "adapter_disabled: push provider is not configured") }
class ConsolePushAdapter : PushAdapter {
    override fun send(device: NotificationDeviceResponse, notification: NotificationEventResponse): ProviderResult {
        val id = "console-${UUID.randomUUID()}"
        val m = jacksonObjectMapper()
        println("T30_PUSH_CONSOLE payload=" + m.writeValueAsString(mapOf("deviceId" to device.id, "provider" to "console", "title" to notification.title, "body" to notification.body, "notificationId" to notification.id, "providerMessageId" to id)))
        return ProviderResult("sent", id)
    }
}
class WebhookPushAdapter(private val url: java.net.URI) : PushAdapter {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    override fun send(device: NotificationDeviceResponse, notification: NotificationEventResponse): ProviderResult = try {
        val body = jacksonObjectMapper().writeValueAsString(mapOf("deviceId" to device.id, "provider" to device.pushProvider, "title" to notification.title, "body" to notification.body, "notificationId" to notification.id))
        val r = client.send(HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json").header("Idempotency-Key", notification.id.toString()).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
        if (r.statusCode() in 200..299) ProviderResult("sent", Regex("\"messageId\"\\s*:\\s*\"([^\"]+)\"").find(r.body())?.groupValues?.get(1) ?: UUID.randomUUID().toString()) else ProviderResult("failed", errorMessage = "provider_error: HTTP ${r.statusCode()}")
    } catch (e: Exception) { ProviderResult("failed", errorMessage = "provider_error: ${e::class.simpleName}: ${e.message}") }
}
class NotificationDispatcher(private val config: NotificationConfig, private val coreApiClient: CoreApiClient) {
    private val firstByGroup = ConcurrentHashMap<String, Pair<OffsetDateTime, UUID>>()
    private val adapter: PushAdapter = when (config.pushProvider) { "console" -> ConsolePushAdapter(); "webhook" -> config.webhookUrl?.let(::WebhookPushAdapter) ?: NonePushAdapter(); else -> NonePushAdapter() }
    fun dispatchOnce(): Int {
        val claimed = coreApiClient.claimNotifications()
        claimed.items.forEach { item ->
            val result = dispatch(item)
            println("T30_PUSH_DECISION notificationId=${item.notification.id} status=${result.status} reason=${result.errorMessage ?: result.providerMessageId ?: "none"}")
            coreApiClient.markDispatchResult(item.notification.id, result)
        }
        return claimed.items.size
    }
    private fun dispatch(item: NotificationDispatchItem): NotificationDispatchResultRequest {
        if (item.preference?.enabled == false) return NotificationDispatchResultRequest("suppressed", errorMessage = "preference_disabled")
        if (item.preference?.channels?.get("push")?.asBoolean(true) == false) return NotificationDispatchResultRequest("suppressed", errorMessage = "push_channel_disabled")
        if (inQuietHours(item.preference?.quietHours)) return NotificationDispatchResultRequest("suppressed", errorMessage = "quiet_hours")
        val device = item.recipientDevice ?: return NotificationDispatchResultRequest("failed", errorMessage = "no_device: no push-capable device is registered")
        val key = "${item.notification.familyId}:${item.notification.recipientUserId}:${item.notification.type}"
        val previous = firstByGroup[key]
        if (previous != null && Duration.between(previous.first, item.notification.createdAt).seconds < config.mergeWindowSeconds) return NotificationDispatchResultRequest("suppressed", errorMessage = "merged:${previous.second}")
        firstByGroup[key] = item.notification.createdAt to item.notification.id
        if (!config.dispatchEnabled) return NotificationDispatchResultRequest("failed", errorMessage = "adapter_disabled: dispatch is disabled")
        val r = adapter.send(device, item.notification)
        return NotificationDispatchResultRequest(r.status, r.providerMessageId, r.errorMessage)
    }
    private fun inQuietHours(node: tools.jackson.databind.JsonNode?): Boolean {
        if (node == null || node.isEmpty || node.get("enabled")?.asBoolean(true) == false) return false
        val start = node.get("start")?.asText()?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return false
        val end = node.get("end")?.asText()?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return false
        val zone = runCatching { ZoneId.of(node.get("timezone")?.asText() ?: "Asia/Shanghai") }.getOrDefault(ZoneId.of("Asia/Shanghai"))
        val time = OffsetDateTime.now(zone).toLocalTime()
        return if (start <= end) time >= start && time < end else time >= start || time < end
    }
}
