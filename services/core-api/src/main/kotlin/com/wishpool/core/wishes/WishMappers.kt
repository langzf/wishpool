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
        fragmentVisualMode = rs.getString("fragment_visual_mode"),
        fragmentGridRows = rs.getInt("fragment_grid_rows").takeUnless { rs.wasNull() },
        fragmentGridCols = rs.getInt("fragment_grid_cols").takeUnless { rs.wasNull() },
        fragmentMaskJson = rs.getString("fragment_mask_json"),
        fragmentLitJson = rs.getString("fragment_lit_json"),
    )

fun wishImageGenerationJobRecord(rs: ResultSet, rowNum: Int): WishImageGenerationJobRecord =
    WishImageGenerationJobRecord(
        id = rs.getObject("id", UUID::class.java),
        familyId = rs.getObject("family_id", UUID::class.java),
        childId = rs.getObject("child_id", UUID::class.java),
        wishId = rs.getObject("wish_id", UUID::class.java),
        usageCode = rs.getString("usage_code"),
        providerCode = rs.getString("provider_code"),
        title = rs.getString("title_snapshot"),
        note = rs.getString("note_snapshot"),
        category = rs.getString("category"),
        style = rs.getString("style"),
        aspectRatio = rs.getString("aspect_ratio"),
        status = rs.getString("status"),
        mediaAssetId = rs.getObject("media_asset_id", UUID::class.java),
        modelProvider = rs.getString("model_provider"),
        modelName = rs.getString("model_name"),
        prompt = rs.getString("prompt"),
        attemptCount = rs.getInt("attempt_count"),
        errorCode = rs.getString("error_code"),
        errorMessage = rs.getString("error_message"),
        createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
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
