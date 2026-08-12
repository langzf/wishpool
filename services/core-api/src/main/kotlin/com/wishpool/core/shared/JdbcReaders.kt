package com.wishpool.core.shared

import java.sql.ResultSet

fun ResultSet.nullableInt(column: String): Int? {
    val value = getInt(column)
    return if (wasNull()) null else value
}
