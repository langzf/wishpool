package com.wishpool.core.sync

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class FamilyEventResponse(
    val seq: Long,
    val familyId: UUID,
    val type: String,
    val aggregateType: String,
    val aggregateId: UUID,
    val occurredAt: OffsetDateTime,
    val payload: JsonNode,
)

data class SyncPullResponse(
    val events: List<FamilyEventResponse>,
    val latestSeq: Long,
)

data class FamilyEventRecord(
    val id: UUID,
    val familyId: UUID,
    val seq: Long,
    val type: String,
    val aggregateType: String,
    val aggregateId: UUID,
    val occurredAt: OffsetDateTime,
    val payload: JsonNode,
)
