package com.wishpool.core.outbox

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class ClaimOutboxEventsRequest(
    val limit: Int = 50,
    val leaseSeconds: Long? = null,
)

data class RetryOutboxEventRequest(
    val delaySeconds: Long = 60,
    val reason: String? = null,
)

data class OutboxClaimResponse(
    val events: List<OutboxEventResponse>,
)

data class OutboxEventResponse(
    val id: UUID,
    val type: String,
    val aggregateType: String,
    val aggregateId: UUID,
    val payload: JsonNode,
    val availableAt: OffsetDateTime,
    val leasedUntil: OffsetDateTime?,
    val retryCount: Int,
    val createdAt: OffsetDateTime,
)

data class OutboxAckResponse(
    val id: UUID,
    val status: String,
    val publishedAt: OffsetDateTime?,
)
