package com.wishpool.core.ai

import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun aiPrecheckRecord(objectMapper: ObjectMapper): (ResultSet, Int) -> AiPrecheckRecord = { rs, _ ->
    AiPrecheckRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        aiJobId = rs.getObject("ai_job_id", UUID::class.java),
        submissionId = rs.getObject("submission_id", UUID::class.java),
        type = rs.getString("type"),
        summary = rs.getString("summary"),
        confidence = rs.getBigDecimal("confidence")?.toDouble(),
        flags = objectMapper.readTree(rs.getString("flags_json")),
        modelProvider = rs.getString("model_provider"),
        modelName = rs.getString("model_name"),
        modelVersion = rs.getString("model_version"),
        promptVersion = rs.getString("prompt_version"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
    )
}
