package com.wishpool.core.ai

import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.NotFoundError
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class ImageModelProviderService(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
    private val currentUser: CurrentUser,
) {
    fun listAdminProviders(): List<ImageModelProviderResponse> =
        listProviderRecords(includeDisabled = true).map(::toResponse)

    fun listUsageMappings(): List<ImageGenUsageResponse> =
        jdbcClient.sql(
            """
            select u.usage_code, u.provider_code, p.display_name as provider_display_name,
                   u.created_at, u.updated_at
            from image_gen_usage u
            left join image_model_provider p on p.code = u.provider_code
            order by u.usage_code
            """.trimIndent(),
        )
            .query(::imageGenUsageRecord)
            .list()
            .map(::toUsageResponse)

    fun listBusinessProviders(usageCode: String = WISH_CARD_USAGE): BusinessImageModelProviderListResponse {
        currentUser.require()
        val normalizedUsage = normalizeUsageCode(usageCode)
        val providers = listProviderRecords(includeDisabled = false)
        return BusinessImageModelProviderListResponse(
            usageCode = normalizedUsage,
            selectedProviderCode = resolveProviderRecord(providerCode = null, usageCode = normalizedUsage)?.code,
            providers = providers.map(::toResponse),
        )
    }

    @Transactional
    fun createProvider(request: ImageModelProviderWriteRequest): ImageModelProviderResponse {
        val code = normalizeCode(request.code ?: throw BadRequestError("code is required."))
        validateWriteRequest(request, requireCode = true)
        if (request.isDefault && !request.isEnabled) throw BadRequestError("Default provider must be enabled.")
        if (request.isDefault) clearDefault()
        return try {
            jdbcClient.sql(
                """
                insert into image_model_provider (
                  code, display_name, provider_type, base_url, api_key, model_name,
                  extra_params, is_default, is_enabled
                ) values (
                  :code, :display_name, :provider_type, :base_url, :api_key, :model_name,
                  cast(:extra_params as jsonb), :is_default, :is_enabled
                )
                returning ${providerSelectColumns()}
                """.trimIndent(),
            )
                .param("code", code)
                .param("display_name", request.displayName.trim())
                .param("provider_type", request.providerType)
                .param("base_url", request.baseUrl.trim().trimEnd('/'))
                .param("api_key", cleanApiKey(request.apiKey))
                .param("model_name", request.modelName.trim())
                .param("extra_params", jsonString(request.extraParams))
                .param("is_default", request.isDefault)
                .param("is_enabled", request.isEnabled)
                .query(imageModelProviderRecord(objectMapper))
                .single()
                .let(::toResponse)
        } catch (ex: DuplicateKeyException) {
            throw ConflictError("Image model provider code already exists.")
        }
    }

    @Transactional
    fun updateProvider(id: UUID, request: ImageModelProviderWriteRequest): ImageModelProviderResponse {
        validateWriteRequest(request, requireCode = false)
        val existing = findProviderById(id) ?: throw NotFoundError("Image model provider not found.")
        if (request.isDefault && !request.isEnabled) throw BadRequestError("Default provider must be enabled.")
        if (!request.isEnabled && existing.isDefault) throw ConflictError("Set another provider as default before disabling this one.")
        if (request.isDefault) clearDefault(exceptId = id)
        val apiKey = cleanApiKey(request.apiKey) ?: existing.apiKey
        return jdbcClient.sql(
            """
            update image_model_provider
            set display_name = :display_name,
                provider_type = :provider_type,
                base_url = :base_url,
                api_key = :api_key,
                model_name = :model_name,
                extra_params = cast(:extra_params as jsonb),
                is_default = :is_default,
                is_enabled = :is_enabled,
                updated_at = now()
            where id = :id
            returning ${providerSelectColumns()}
            """.trimIndent(),
        )
            .param("id", id)
            .param("display_name", request.displayName.trim())
            .param("provider_type", request.providerType)
            .param("base_url", request.baseUrl.trim().trimEnd('/'))
            .param("api_key", apiKey)
            .param("model_name", request.modelName.trim())
            .param("extra_params", jsonString(request.extraParams))
            .param("is_default", request.isDefault)
            .param("is_enabled", request.isEnabled)
            .query(imageModelProviderRecord(objectMapper))
            .single()
            .let(::toResponse)
    }

    @Transactional
    fun toggleProvider(id: UUID, request: ImageModelProviderToggleRequest): ImageModelProviderResponse {
        val existing = findProviderById(id) ?: throw NotFoundError("Image model provider not found.")
        if (!request.isEnabled && existing.isDefault) {
            throw ConflictError("Set another provider as default before disabling this one.")
        }
        return jdbcClient.sql(
            """
            update image_model_provider
            set is_enabled = :is_enabled,
                updated_at = now()
            where id = :id
            returning ${providerSelectColumns()}
            """.trimIndent(),
        )
            .param("id", id)
            .param("is_enabled", request.isEnabled)
            .query(imageModelProviderRecord(objectMapper))
            .single()
            .let(::toResponse)
    }

    @Transactional
    fun setDefaultProvider(id: UUID): ImageModelProviderResponse {
        val existing = findProviderById(id) ?: throw NotFoundError("Image model provider not found.")
        if (!existing.isEnabled) throw ConflictError("Only enabled providers can be set as default.")
        clearDefault(exceptId = id)
        return jdbcClient.sql(
            """
            update image_model_provider
            set is_default = true,
                updated_at = now()
            where id = :id
            returning ${providerSelectColumns()}
            """.trimIndent(),
        )
            .param("id", id)
            .query(imageModelProviderRecord(objectMapper))
            .single()
            .let(::toResponse)
    }

    @Transactional
    fun deleteProvider(id: UUID) {
        val existing = findProviderById(id) ?: throw NotFoundError("Image model provider not found.")
        if (existing.isDefault) throw ConflictError("Default provider cannot be deleted.")
        val usageCount = jdbcClient.sql("select count(*) from image_gen_usage where provider_code = :provider_code")
            .param("provider_code", existing.code)
            .query(Int::class.java)
            .single()
        if (usageCount > 0) throw ConflictError("Provider is used by a business usage mapping.")
        val jobCount = jdbcClient.sql("select count(*) from wish_image_generation_job where provider_code = :provider_code")
            .param("provider_code", existing.code)
            .query(Int::class.java)
            .single()
        if (jobCount > 0) throw ConflictError("Provider is used by image generation jobs.")
        jdbcClient.sql("delete from image_model_provider where id = :id")
            .param("id", id)
            .update()
    }

    @Transactional
    fun upsertUsageMapping(request: ImageGenUsageWriteRequest): ImageGenUsageResponse {
        val usageCode = normalizeUsageCode(request.usageCode)
        val provider = findProviderByCode(request.providerCode) ?: throw NotFoundError("Image model provider not found.")
        if (!provider.isEnabled) throw ConflictError("Only enabled providers can be assigned to a usage.")
        return jdbcClient.sql(
            """
            insert into image_gen_usage (usage_code, provider_code)
            values (:usage_code, :provider_code)
            on conflict (usage_code) do update set
              provider_code = excluded.provider_code,
              updated_at = now()
            returning usage_code, provider_code,
                      (select display_name from image_model_provider where code = image_gen_usage.provider_code) as provider_display_name,
                      created_at, updated_at
            """.trimIndent(),
        )
            .param("usage_code", usageCode)
            .param("provider_code", provider.code)
            .query(::imageGenUsageRecord)
            .single()
            .let(::toUsageResponse)
    }

    fun resolveProviderRecord(providerCode: String?, usageCode: String = WISH_CARD_USAGE): ImageModelProviderRecord? {
        if (!providerCode.isNullOrBlank()) {
            return findEnabledProviderByCode(providerCode.trim())
                ?: throw NotFoundError("Requested image model provider is not enabled.")
        }
        val usageProviderCode = jdbcClient.sql(
            """
            select u.provider_code
            from image_gen_usage u
            join image_model_provider p on p.code = u.provider_code
            where u.usage_code = :usage_code
              and p.is_enabled = true
            """.trimIndent(),
        )
            .param("usage_code", normalizeUsageCode(usageCode))
            .query(String::class.java)
            .optional()
            .orElse(null)
        if (usageProviderCode != null) return findEnabledProviderByCode(usageProviderCode)
        return jdbcClient.sql(
            """
            select ${providerSelectColumns()}
            from image_model_provider
            where is_default = true
              and is_enabled = true
            """.trimIndent(),
        )
            .query(imageModelProviderRecord(objectMapper))
            .optional()
            .orElse(null)
    }

    @Suppress("UNCHECKED_CAST")
    fun toWorkerConfig(record: ImageModelProviderRecord): AiWorkerImageProviderConfig =
        AiWorkerImageProviderConfig(
            code = record.code,
            provider_type = record.providerType,
            base_url = record.baseUrl,
            api_key = record.apiKey,
            model_name = record.modelName,
            extra_params = objectMapper.convertValue(record.extraParams, Map::class.java) as Map<String, Any?>,
        )

    private fun listProviderRecords(includeDisabled: Boolean): List<ImageModelProviderRecord> {
        val enabledClause = if (includeDisabled) "" else "where is_enabled = true"
        return jdbcClient.sql(
            """
            select ${providerSelectColumns()}
            from image_model_provider
            $enabledClause
            order by is_default desc, is_enabled desc, updated_at desc, display_name asc
            """.trimIndent(),
        )
            .query(imageModelProviderRecord(objectMapper))
            .list()
    }

    private fun findProviderById(id: UUID): ImageModelProviderRecord? =
        jdbcClient.sql(
            """
            select ${providerSelectColumns()}
            from image_model_provider
            where id = :id
            """.trimIndent(),
        )
            .param("id", id)
            .query(imageModelProviderRecord(objectMapper))
            .optional()
            .orElse(null)

    private fun findProviderByCode(code: String): ImageModelProviderRecord? =
        jdbcClient.sql(
            """
            select ${providerSelectColumns()}
            from image_model_provider
            where code = :code
            """.trimIndent(),
        )
            .param("code", normalizeCode(code))
            .query(imageModelProviderRecord(objectMapper))
            .optional()
            .orElse(null)

    private fun findEnabledProviderByCode(code: String): ImageModelProviderRecord? =
        jdbcClient.sql(
            """
            select ${providerSelectColumns()}
            from image_model_provider
            where code = :code
              and is_enabled = true
            """.trimIndent(),
        )
            .param("code", normalizeCode(code))
            .query(imageModelProviderRecord(objectMapper))
            .optional()
            .orElse(null)

    private fun validateWriteRequest(request: ImageModelProviderWriteRequest, requireCode: Boolean) {
        if (requireCode && request.code.isNullOrBlank()) throw BadRequestError("code is required.")
        if (!request.code.isNullOrBlank()) normalizeCode(request.code)
        if (request.displayName.isBlank()) throw BadRequestError("displayName cannot be blank.")
        if (request.providerType !in PROVIDER_TYPES) throw BadRequestError("Unsupported providerType.")
        if (request.baseUrl.isBlank()) throw BadRequestError("baseUrl cannot be blank.")
        if (request.modelName.isBlank()) throw BadRequestError("modelName cannot be blank.")
        if (request.extraParams != null && !request.extraParams.isObject) throw BadRequestError("extraParams must be a JSON object.")
    }

    private fun clearDefault(exceptId: UUID? = null) {
        val sql = if (exceptId == null) {
            "update image_model_provider set is_default = false, updated_at = now() where is_default = true"
        } else {
            "update image_model_provider set is_default = false, updated_at = now() where is_default = true and id <> :id"
        }
        val spec = jdbcClient.sql(sql)
        if (exceptId != null) spec.param("id", exceptId)
        spec.update()
    }

    private fun toResponse(record: ImageModelProviderRecord): ImageModelProviderResponse =
        ImageModelProviderResponse(
            id = record.id,
            code = record.code,
            displayName = record.displayName,
            providerType = record.providerType,
            baseUrl = record.baseUrl,
            apiKeyMasked = maskApiKey(record.apiKey),
            modelName = record.modelName,
            extraParams = record.extraParams,
            isDefault = record.isDefault,
            isEnabled = record.isEnabled,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
        )

    private fun toUsageResponse(record: ImageGenUsageRecord): ImageGenUsageResponse =
        ImageGenUsageResponse(
            usageCode = record.usageCode,
            providerCode = record.providerCode,
            providerDisplayName = record.providerDisplayName,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
        )

    private fun jsonString(node: JsonNode?): String =
        objectMapper.writeValueAsString(node ?: objectMapper.createObjectNode())

    private fun cleanApiKey(value: String?): String? =
        value?.trim()?.takeIf { it.isNotBlank() }

    private fun maskApiKey(value: String?): String? {
        val key = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val suffix = key.takeLast(3)
        val prefix = key.take(3).takeIf { key.length > 6 } ?: "key"
        return "$prefix-***$suffix"
    }

    private fun normalizeCode(value: String?): String {
        val code = value?.trim()?.lowercase() ?: throw BadRequestError("code is required.")
        if (!CODE_REGEX.matches(code)) throw BadRequestError("code must use lowercase letters, digits, and hyphens.")
        return code
    }

    private fun normalizeUsageCode(value: String): String {
        val usageCode = value.trim().lowercase()
        if (!USAGE_CODE_REGEX.matches(usageCode)) throw BadRequestError("usageCode must use lowercase letters, digits, and underscores.")
        return usageCode
    }

    private fun providerSelectColumns(): String =
        """
        id, code, display_name, provider_type, base_url, api_key, model_name,
        extra_params::text as extra_params_json, is_default, is_enabled, created_at, updated_at
        """.trimIndent()

    private companion object {
        const val WISH_CARD_USAGE = "wish_card"
        val PROVIDER_TYPES = setOf("volcengine_ark", "aliyun_bailian", "siliconflow", "deterministic", "custom_openai_compatible")
        val CODE_REGEX = Regex("^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$")
        val USAGE_CODE_REGEX = Regex("^[a-z0-9][a-z0-9_]{1,62}[a-z0-9]$")
    }
}
