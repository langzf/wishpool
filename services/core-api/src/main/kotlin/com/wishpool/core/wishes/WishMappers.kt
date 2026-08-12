package com.wishpool.core.wishes

import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

fun wishRecord(rs: ResultSet, rowNum: Int): WishRecord =
    WishRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        weekId = rs.getString("week_id"),
        title = rs.getString("title"),
        note = rs.getString("note"),
        imageMediaId = rs.getObject("image_media_id", UUID::class.java),
        requiredFragments = rs.getInt("required_fragments"),
        earnedFragments = rs.getInt("earned_fragments"),
        rewardMode = rs.getString("reward_mode"),
        status = rs.getString("status"),
    )

fun wishRedemptionRecord(rs: ResultSet, rowNum: Int): WishRedemptionRecord =
    WishRedemptionRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        wishId = rs.getObject("wish_id", UUID::class.java),
        redeemedDate = rs.getDate("redeemed_date").toLocalDate(),
        parentNote = rs.getString("parent_note"),
        childNote = rs.getString("child_note"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
    )
