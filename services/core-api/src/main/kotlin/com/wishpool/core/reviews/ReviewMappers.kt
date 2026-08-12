package com.wishpool.core.reviews

import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun reviewRecord(rs: ResultSet, rowNum: Int): ReviewRecord =
    ReviewRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        submissionId = rs.getObject("submission_id", UUID::class.java),
        taskInstanceId = rs.getObject("task_instance_id", UUID::class.java),
        decision = rs.getString("decision"),
        reviewedBy = rs.getObject("reviewed_by", UUID::class.java),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        revokedAt = rs.getObject("revoked_at", OffsetDateTime::class.java),
        feedbackEmoji = rs.getString("feedback_emoji"),
        feedbackText = rs.getString("feedback_text"),
        audioMediaId = rs.getObject("audio_media_id", UUID::class.java),
    )
