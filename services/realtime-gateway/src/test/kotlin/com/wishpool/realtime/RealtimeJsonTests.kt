package com.wishpool.realtime

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class RealtimeJsonTests {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `reads sync pull response with time and payload tree`() {
        val familyId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val json = """
            {
              "events": [
                {
                  "seq": 7,
                  "familyId": "$familyId",
                  "type": "review.approved",
                  "aggregateType": "review",
                  "aggregateId": "$aggregateId",
                  "occurredAt": "2026-08-12T11:06:00Z",
                  "payload": {
                    "childId": "${UUID.randomUUID()}",
                    "stars": 3
                  }
                }
              ],
              "latestSeq": 7
            }
        """.trimIndent()

        val response = mapper.readValue<SyncPullResponse>(json)

        assertEquals(7, response.latestSeq)
        assertEquals("review.approved", response.events.single().type)
        assertEquals(3, response.events.single().payload.path("stars").asInt())
        assertEquals(OffsetDateTime.parse("2026-08-12T11:06:00Z"), response.events.single().occurredAt)
    }

    @Test
    fun `writes realtime event envelope`() {
        val familyId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val event = FamilyEvent(
            seq = 9,
            familyId = familyId,
            type = "task.created",
            aggregateType = "task",
            aggregateId = aggregateId,
            occurredAt = OffsetDateTime.parse("2026-08-12T12:00:00Z"),
            payload = mapper.createObjectNode().put("title", "Brush teeth"),
        )

        val json = mapper.writeValueAsString(RealtimeFamilyEventMessage(event = event))
        val tree = mapper.readTree(json)

        assertEquals("family.event", tree.path("type").asString())
        assertEquals(9, tree.path("event").path("seq").asLong())
        assertEquals("Brush teeth", tree.path("event").path("payload").path("title").asString())
    }

    @Test
    fun `reads me response from core api shape`() {
        val userId = UUID.randomUUID()
        val familyId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val json = """
            {
              "user": {
                "id": "$userId",
                "status": "active",
                "displayName": "Parent",
                "avatarUrl": null
              },
              "families": [
                {
                  "family": {
                    "id": "$familyId",
                    "name": "Smoke Family",
                    "timezone": "Asia/Shanghai",
                    "status": "active"
                  },
                  "member": {
                    "id": "$memberId",
                    "familyId": "$familyId",
                    "userId": "$userId",
                    "childId": null,
                    "role": "parent_owner",
                    "displayName": "Parent",
                    "status": "active"
                  }
                }
              ]
            }
        """.trimIndent()

        val response = mapper.readValue<MeResponse>(json)

        assertEquals(userId, response.user.id)
        assertEquals(familyId, response.families.single().family.id)
        assertEquals("parent_owner", response.families.single().member.role)
    }
}
