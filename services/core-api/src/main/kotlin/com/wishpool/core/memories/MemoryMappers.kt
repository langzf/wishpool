package com.wishpool.core.memories

import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.util.UUID

fun weeklyMemoryRecord(objectMapper: ObjectMapper): (ResultSet, Int) -> WeeklyMemoryRecord = { rs, _ ->
    WeeklyMemoryRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        weekId = rs.getString("week_id"),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date").toLocalDate(),
        title = rs.getString("title"),
        summary = objectMapper.readTree(rs.getString("summary_json")),
        status = rs.getString("status"),
    )
}
