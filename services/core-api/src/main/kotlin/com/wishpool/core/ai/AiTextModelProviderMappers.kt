package com.wishpool.core.ai
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID
fun aiTextModelProviderRecord(mapper:ObjectMapper):(ResultSet,Int)->AiTextModelProviderRecord={rs,_ -> AiTextModelProviderRecord(rs.getObject("id",UUID::class.java),rs.getString("code"),rs.getString("display_name"),rs.getString("provider_type"),rs.getString("base_url"),rs.getString("api_key"),rs.getString("model_name"),rs.getString("capability"),mapper.readTree(rs.getString("extra_params_json")),rs.getBoolean("is_default"),rs.getBoolean("is_enabled"),rs.getObject("created_at",OffsetDateTime::class.java),rs.getObject("updated_at",OffsetDateTime::class.java))}
