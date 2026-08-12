package com.wishpool.core.submissions

import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun submissionRecord(rs: ResultSet, rowNum: Int): SubmissionRecord =
    SubmissionRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        taskInstanceId = rs.getObject("task_instance_id", UUID::class.java),
        attemptNo = rs.getInt("attempt_no"),
        submissionType = rs.getString("submission_type"),
        status = rs.getString("status"),
        submittedAt = rs.getObject("submitted_at", OffsetDateTime::class.java),
    )
