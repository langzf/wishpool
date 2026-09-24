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
        // A disabled dispatcher is a local/development sink, not evidence that a
        // provider accepted the notification. Keep the event visible for retry
        // instead of recording a false delivery.
        if (!config.dispatchEnabled) return "failed"
        val device = item.recipientDevice ?: return "failed"
        if (device.pushProvider.isNullOrBlank() || device.pushToken.isNullOrBlank()) return "failed"
        // Provider adapters (APNs/FCM/vendor push) are not implemented yet.
        // Never claim delivery merely because a token is present.
        return "failed"
    }
}
