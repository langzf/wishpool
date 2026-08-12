package com.wishpool.core.idempotency

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IdempotencyService(
    private val jdbcClient: JdbcClient,
) {
    fun find(
        familyId: UUID,
        key: String?,
        operation: String,
    ): IdempotencyRecord? {
        if (key.isNullOrBlank()) return null
        return jdbcClient.sql(
            """
            select family_id, key, operation, resource_type, resource_id
            from idempotency_record
            where family_id = :family_id
              and key = :key
              and operation = :operation
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("key", key.trim())
            .param("operation", operation)
            .query { rs, _ ->
                IdempotencyRecord(
                    familyId = rs.getObject("family_id", UUID::class.java),
                    key = rs.getString("key"),
                    operation = rs.getString("operation"),
                    resourceType = rs.getString("resource_type"),
                    resourceId = rs.getObject("resource_id", UUID::class.java),
                )
            }
            .optional()
            .orElse(null)
    }

    fun remember(
        familyId: UUID,
        key: String?,
        operation: String,
        resourceType: String,
        resourceId: UUID,
        createdBy: UUID,
    ) {
        if (key.isNullOrBlank()) return
        try {
            jdbcClient.sql(
                """
                insert into idempotency_record (
                  family_id, key, operation, resource_type, resource_id, created_by
                ) values (
                  :family_id, :key, :operation, :resource_type, :resource_id, :created_by
                )
                """.trimIndent(),
            )
                .param("family_id", familyId)
                .param("key", key.trim())
                .param("operation", operation)
                .param("resource_type", resourceType)
                .param("resource_id", resourceId)
                .param("created_by", createdBy)
                .update()
        } catch (ex: DuplicateKeyException) {
            // A concurrent duplicate completed first; the caller will resolve it on retry.
        }
    }
}

data class IdempotencyRecord(
    val familyId: UUID,
    val key: String,
    val operation: String,
    val resourceType: String,
    val resourceId: UUID,
)
