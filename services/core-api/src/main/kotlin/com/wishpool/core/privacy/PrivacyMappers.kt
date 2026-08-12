package com.wishpool.core.privacy

import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun privacyRequestRecord(rs: ResultSet, rowNum: Int): PrivacyRequestRecord =
    PrivacyRequestRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        requestType = rs.getString("request_type"),
        status = rs.getString("status"),
        requestedBy = rs.getObject("requested_by", UUID::class.java),
        exportMediaId = rs.getObject("export_media_id", UUID::class.java),
        reason = rs.getString("reason"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
    )
