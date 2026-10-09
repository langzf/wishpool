package com.wishpool.core.ai
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID
data class AiTextModelProviderResponse(val id: UUID,val code:String,val displayName:String,val providerType:String,val baseUrl:String,val apiKeyMasked:String?,val hasApiKey:Boolean,val modelName:String,val capability:String,val extraParams:JsonNode,val isDefault:Boolean,val isEnabled:Boolean,val createdAt:OffsetDateTime,val updatedAt:OffsetDateTime)
data class AiTextModelProviderWriteRequest(val code:String?=null,val displayName:String,val providerType:String,val baseUrl:String,val apiKey:String?=null,val modelName:String,val capability:String,val extraParams:JsonNode?=null,val isDefault:Boolean=false,val isEnabled:Boolean=true)
data class AiTextModelProviderToggleRequest(val isEnabled:Boolean)
data class AiTextModelProviderRecord(val id:UUID,val code:String,val displayName:String,val providerType:String,val baseUrl:String,val apiKey:String?,val modelName:String,val capability:String,val extraParams:JsonNode,val isDefault:Boolean,val isEnabled:Boolean,val createdAt:OffsetDateTime,val updatedAt:OffsetDateTime)
data class AiTextProviderTestResponse(val ok:Boolean,val message:String)
