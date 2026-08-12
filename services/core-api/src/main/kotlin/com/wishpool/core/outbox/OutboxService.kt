package com.wishpool.core.outbox

import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@Service
class OutboxService(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
    @Value("\${wishpool.outbox.default-lease-seconds}") private val defaultLeaseSeconds: Long,
) {
    @Transactional
    fun claim(request: ClaimOutboxEventsRequest): OutboxClaimResponse {
        val limit = request.limit.coerceIn(1, MAX_LIMIT)
        val leaseSeconds = (request.leaseSeconds ?: defaultLeaseSeconds).coerceIn(MIN_LEASE_SECONDS, MAX_LEASE_SECONDS)
        val events = jdbcClient.sql(
            """
            with claimable as (
              select id
              from outbox_event
              where published_at is null
                and available_at <= now()
                and (leased_until is null or leased_until <= now())
              order by available_at, created_at
              limit :limit
              for update skip locked
            )
            update outbox_event e
            set leased_until = now() + (:lease_seconds * interval '1 second'),
                retry_count = retry_count + 1,
                last_error = null
            from claimable
            where e.id = claimable.id
            returning e.id, e.event_type, e.aggregate_type, e.aggregate_id, e.payload_json::text,
                      e.available_at, e.leased_until, e.retry_count, e.created_at
            """.trimIndent(),
        )
            .param("limit", limit)
            .param("lease_seconds", leaseSeconds)
            .query(::outboxEventResponse)
            .list()
        return OutboxClaimResponse(events)
    }

    @Transactional
    fun markPublished(eventId: UUID): OutboxAckResponse {
        val published = jdbcClient.sql(
            """
            update outbox_event
            set published_at = coalesce(published_at, now()),
                leased_until = null,
                last_error = null
            where id = :id
            returning id, published_at
            """.trimIndent(),
        )
            .param("id", eventId)
            .query { rs, _ ->
                OutboxAckResponse(
                    id = rs.getObject("id", UUID::class.java),
                    status = "published",
                    publishedAt = rs.getObject("published_at", OffsetDateTime::class.java),
                )
            }
            .optional()
            .orElseThrow { NotFoundError("Outbox event not found.") }
        return published
    }

    @Transactional
    fun scheduleRetry(eventId: UUID, request: RetryOutboxEventRequest): OutboxEventResponse {
        if (request.delaySeconds < 0) throw BadRequestError("delaySeconds must be greater than or equal to 0.")
        return jdbcClient.sql(
            """
            update outbox_event
            set available_at = now() + (:delay_seconds * interval '1 second'),
                leased_until = null,
                last_error = :reason
            where id = :id
              and published_at is null
            returning id, event_type, aggregate_type, aggregate_id, payload_json::text,
                      available_at, leased_until, retry_count, created_at
            """.trimIndent(),
        )
            .param("id", eventId)
            .param("delay_seconds", request.delaySeconds.coerceAtMost(MAX_RETRY_DELAY_SECONDS))
            .param("reason", request.reason?.trim())
            .query(::outboxEventResponse)
            .optional()
            .orElseThrow { NotFoundError("Unpublished outbox event not found.") }
    }

    private fun outboxEventResponse(rs: java.sql.ResultSet, rowNum: Int): OutboxEventResponse =
        OutboxEventResponse(
            id = rs.getObject("id", UUID::class.java),
            type = rs.getString("event_type"),
            aggregateType = rs.getString("aggregate_type"),
            aggregateId = rs.getObject("aggregate_id", UUID::class.java),
            payload = objectMapper.readTree(rs.getString("payload_json")),
            availableAt = rs.getObject("available_at", OffsetDateTime::class.java),
            leasedUntil = rs.getObject("leased_until", OffsetDateTime::class.java),
            retryCount = rs.getInt("retry_count"),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        )

    private companion object {
        const val MAX_LIMIT = 500
        const val MIN_LEASE_SECONDS = 5L
        const val MAX_LEASE_SECONDS = 900L
        const val MAX_RETRY_DELAY_SECONDS = 86_400L
    }
}
