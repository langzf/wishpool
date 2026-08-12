package com.wishpool.workflow

import tools.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.optionalUuid(): UUID? =
    takeUnless { it.isMissingNode || it.isNull }
        ?.asString()
        ?.takeIf { it.isNotBlank() }
        ?.let(UUID::fromString)

fun JsonNode.requiredUuid(fieldName: String): UUID =
    optionalUuid() ?: throw IllegalArgumentException("Missing UUID field: $fieldName")
