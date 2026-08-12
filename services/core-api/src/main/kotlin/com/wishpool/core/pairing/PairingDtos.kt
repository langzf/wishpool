package com.wishpool.core.pairing

import com.wishpool.core.auth.DeviceRegistration
import java.time.OffsetDateTime
import java.util.UUID

data class CreatePairingSessionRequest(
    val childId: UUID,
)

data class PairingSessionCreated(
    val sessionId: UUID,
    val pairingCode: String,
    val expiresAt: OffsetDateTime,
)

data class ConsumePairingCodeRequest(
    val pairingCode: String,
    val device: DeviceRegistration,
)
