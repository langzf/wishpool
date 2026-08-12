package com.wishpool.core.events

import com.wishpool.core.notifications.NotificationEventProjector
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class DomainEventPublisher(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
    private val notificationEventProjector: NotificationEventProjector,
) {
    fun publishFamilyEvent(
        familyId: UUID,
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payload: Map<String, Any?>,
    ) {
        publishFamilyEvent(
            familyId = familyId,
            eventType = eventType,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            payloadJson = objectMapper.writeValueAsString(payload),
        )
    }

    fun publishFamilyEvent(
        familyId: UUID,
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        payloadJson: String,
    ) {
        val seq = nextFamilySequence(familyId)
        jdbcClient.sql(
            """
            insert into family_event (
              family_id, seq, event_type, aggregate_type, aggregate_id, payload_json
            ) values (
              :family_id, :seq, :event_type, :aggregate_type, :aggregate_id, cast(:payload_json as jsonb)
            )
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("seq", seq)
            .param("event_type", eventType)
            .param("aggregate_type", aggregateType)
            .param("aggregate_id", aggregateId)
            .param("payload_json", payloadJson)
            .update()

        jdbcClient.sql(
            """
            insert into outbox_event (
              event_type, aggregate_type, aggregate_id, payload_json
            ) values (
              :event_type, :aggregate_type, :aggregate_id, cast(:payload_json as jsonb)
            )
            """.trimIndent(),
        )
            .param("event_type", eventType)
            .param("aggregate_type", aggregateType)
            .param("aggregate_id", aggregateId)
            .param("payload_json", payloadJson)
            .update()

        notificationEventProjector.projectFamilyEvent(
            familyId = familyId,
            eventType = eventType,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            payloadJson = payloadJson,
        )
    }

    private fun nextFamilySequence(familyId: UUID): Long {
        jdbcClient.sql(
            """
            select id
            from family
            where id = :family_id
            for update
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .query(UUID::class.java)
            .single()

        return jdbcClient.sql(
            """
            select coalesce(max(seq), 0) + 1
            from family_event
            where family_id = :family_id
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .query(Long::class.java)
            .single()
    }
}
