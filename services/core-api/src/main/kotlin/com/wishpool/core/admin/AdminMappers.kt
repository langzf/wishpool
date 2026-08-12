package com.wishpool.core.admin

import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun adminFamilySummaryRecord(rs: ResultSet, rowNum: Int): AdminFamilySummaryRecord =
    AdminFamilySummaryRecord(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        timezone = rs.getString("timezone"),
        status = rs.getString("status"),
        childCount = rs.getLong("child_count"),
        memberCount = rs.getLong("member_count"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
    )

fun adminPrivacyRequestRecord(rs: ResultSet, rowNum: Int): AdminPrivacyRequestRecord =
    AdminPrivacyRequestRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        requestType = rs.getString("request_type"),
        status = rs.getString("status"),
        requestedBy = rs.getObject("requested_by", UUID::class.java),
        exportMediaId = rs.getObject("export_media_id", UUID::class.java),
        reason = rs.getString("reason"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        completedAt = rs.getObject("completed_at", OffsetDateTime::class.java),
        errorMessage = rs.getString("error_message"),
    )

fun adminAuditLogRecord(objectMapper: ObjectMapper): (ResultSet, Int) -> AdminAuditLogRecord = { rs, _ ->
    AdminAuditLogRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        actorUserId = rs.getObject("actor_user_id", UUID::class.java),
        actorRole = rs.getString("actor_role"),
        action = rs.getString("action"),
        resourceType = rs.getString("resource_type"),
        resourceId = rs.getObject("resource_id", UUID::class.java),
        metadata = objectMapper.readTree(rs.getString("metadata_json")),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
    )
}
