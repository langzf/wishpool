package com.wishpool.core.ai

import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun imageModelProviderRecord(objectMapper: ObjectMapper): (ResultSet, Int) -> ImageModelProviderRecord = { rs, _ ->
    ImageModelProviderRecord(
        id = rs.getObject("id", UUID::class.java),
        code = rs.getString("code"),
        displayName = rs.getString("display_name"),
        providerType = rs.getString("provider_type"),
        baseUrl = rs.getString("base_url"),
        apiKey = rs.getString("api_key"),
        modelName = rs.getString("model_name"),
        extraParams = objectMapper.readTree(rs.getString("extra_params_json")),
        isDefault = rs.getBoolean("is_default"),
        isEnabled = rs.getBoolean("is_enabled"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
    )
}

fun imageGenUsageRecord(rs: ResultSet, rowNum: Int): ImageGenUsageRecord =
    ImageGenUsageRecord(
        usageCode = rs.getString("usage_code"),
        providerCode = rs.getString("provider_code"),
        providerDisplayName = rs.getString("provider_display_name"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
    )
