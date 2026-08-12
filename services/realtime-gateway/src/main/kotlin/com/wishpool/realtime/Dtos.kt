package com.wishpool.realtime

import kotlinx.coroutines.sync.Mutex
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class FamilyEvent(
    val seq: Long,
    val familyId: UUID,
    val type: String,
    val aggregateType: String,
    val aggregateId: UUID,
    val occurredAt: OffsetDateTime,
    val payload: JsonNode,
)

data class SyncPullResponse(
    val events: List<FamilyEvent>,
    val latestSeq: Long,
)

data class MeResponse(
    val user: UserResponse,
    val families: List<FamilyMemberContextResponse>,
)

data class UserResponse(
    val id: UUID,
    val status: String,
    val displayName: String,
    val avatarUrl: String?,
)

data class FamilyMemberContextResponse(
    val family: FamilyResponse,
    val member: FamilyMemberResponse,
)

data class FamilyResponse(
    val id: UUID,
    val name: String,
    val timezone: String,
    val status: String,
)

data class FamilyMemberResponse(
    val id: UUID,
    val familyId: UUID,
    val userId: UUID?,
    val childId: UUID?,
    val role: String,
    val displayName: String,
    val status: String,
)

data class RealtimeConnectionRequest(
    val token: String,
    val familyId: UUID,
    val afterSeq: Long,
    val transport: String,
)

data class RealtimeConnectionContext(
    val connectionId: String,
    val request: RealtimeConnectionRequest,
    val user: UserResponse,
    var latestSeq: Long,
    val syncMutex: Mutex = Mutex(),
)

data class RealtimeConnectedMessage(
    val type: String = "realtime.connected",
    val connectionId: String,
    val familyId: UUID,
    val latestSeq: Long,
)

data class RealtimeHeartbeatMessage(
    val type: String = "realtime.heartbeat",
    val connectionId: String,
    val latestSeq: Long,
    val sentAt: OffsetDateTime,
)

data class RealtimeFamilyEventMessage(
    val type: String = "family.event",
    val event: FamilyEvent,
)

data class RealtimeErrorMessage(
    val type: String = "realtime.error",
    val code: String,
    val message: String,
)

data class RealtimePongMessage(
    val type: String = "realtime.pong",
    val sentAt: OffsetDateTime,
)

data class ClientEnvelope(
    val type: String,
    val afterSeq: Long? = null,
)
