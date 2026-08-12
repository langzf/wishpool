package com.wishpool.core.media

import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun mediaAssetRecord(rs: ResultSet, rowNum: Int): MediaAssetRecord =
    MediaAssetRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        purpose = rs.getString("purpose"),
        storageKey = rs.getString("storage_key"),
        contentType = rs.getString("content_type"),
        sizeBytes = rs.getLong("size_bytes").takeUnless { rs.wasNull() },
        status = rs.getString("status"),
        relatedType = rs.getString("related_type"),
        relatedId = rs.getObject("related_id", UUID::class.java),
    )

fun mediaDerivativeRecord(objectMapper: ObjectMapper): (ResultSet, Int) -> MediaDerivativeRecord = { rs, _ ->
    MediaDerivativeRecord(
        id = rs.getObject("id", UUID::class.java),
        mediaAssetId = rs.getObject("media_asset_id", UUID::class.java),
        kind = rs.getString("kind"),
        storageKey = rs.getString("storage_key"),
        contentType = rs.getString("content_type"),
        sizeBytes = rs.getLong("size_bytes").takeUnless { rs.wasNull() },
        metadata = objectMapper.readTree(rs.getString("metadata_json")),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
    )
}
