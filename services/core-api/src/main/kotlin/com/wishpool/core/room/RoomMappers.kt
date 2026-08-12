package com.wishpool.core.room

import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun roomItemRecord(objectMapper: ObjectMapper): (ResultSet, Int) -> RoomItemRecord = { rs, _ ->
    RoomItemRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        type = rs.getString("type"),
        sourceType = rs.getString("source_type"),
        sourceId = rs.getObject("source_id", UUID::class.java),
        title = rs.getString("title"),
        mediaAssetId = rs.getObject("media_asset_id", UUID::class.java),
        position = objectMapper.readTree(rs.getString("position_json")),
        visible = rs.getBoolean("visible"),
        unlockedAt = rs.getObject("unlocked_at", OffsetDateTime::class.java),
    )
}
