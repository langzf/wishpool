package com.wishpool.notification

class NotificationDispatcher(
    private val config: NotificationConfig,
    private val coreApiClient: CoreApiClient,
) {
    fun dispatchOnce(): Int {
        val claimed = coreApiClient.claimNotifications()
        claimed.items.forEach { item ->
            val status = dispatch(item)
            coreApiClient.markDispatchResult(
                item.notification.id,
                NotificationDispatchResultRequest(
                    status = status,
                    errorMessage = if (status == "failed") "No push-capable device is registered." else null,
                ),
            )
        }
        return claimed.items.size
    }

    private fun dispatch(item: NotificationDispatchItem): String {
        if (item.preference?.enabled == false) return "suppressed"
        val pushEnabled = item.preference?.channels?.get("push")?.asBoolean(true) ?: true
        if (!pushEnabled) return "suppressed"
        if (!config.dispatchEnabled) return "sent"
        val device = item.recipientDevice ?: return "failed"
        if (device.pushProvider.isNullOrBlank() || device.pushToken.isNullOrBlank()) return "failed"
        return "sent"
    }
}
