package com.wishpool.core.wishes

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.idempotency.IdempotencyService
import com.wishpool.core.media.MediaAssetRecord
import com.wishpool.core.media.MediaService
import com.wishpool.core.media.mediaAssetRecord
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.ForbiddenError
import com.wishpool.core.shared.NotFoundError
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class WishService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
    private val imageSuggestionService: WishImageSuggestionService,
    private val eventPublisher: DomainEventPublisher,
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun createWish(request: CreateWishRequest, idempotencyKey: String?): WishResponse {
        val user = currentUser.require()
        familyPolicy.requireParent(user, request.familyId)
        validateCreateWish(request)
        ensureChildBelongsToFamily(request.childId, request.familyId)
        val imageMedia = request.imageMediaId?.let { validateWishImage(it, request.familyId, request.childId) }
        idempotencyService.find(request.familyId, idempotencyKey, CREATE_WISH_OPERATION)
            ?.let { return getWish(it.resourceId) }
        val fragmentGrid = deriveFragmentGrid(request.requiredFragments)
        val fragmentMask = buildFragmentMask(request.fragmentVisualMode, fragmentGrid.first, fragmentGrid.second)
        val fragmentLit = buildFragmentLit(emptyList())

        val wish = jdbcClient.sql(
            """
            insert into wish (
              family_id, child_id, week_id, title, note, image_media_id,
              required_fragments, reward_mode, fragment_visual_mode, fragment_grid_rows, fragment_grid_cols,
              fragment_mask_json, fragment_lit_json,
              status, created_by
            ) values (
              :family_id, :child_id, :week_id, :title, :note, :image_media_id,
              :required_fragments, :reward_mode, :fragment_visual_mode, :fragment_grid_rows, :fragment_grid_cols,
              cast(:fragment_mask_json as jsonb), cast(:fragment_lit_json as jsonb),
              'draft', :created_by
            )
            returning id, family_id, child_id, week_id, title, note, image_media_id,
                      required_fragments, earned_fragments, reward_mode, status,
                      fragment_visual_mode, fragment_grid_rows, fragment_grid_cols,
                      fragment_mask_json::text as fragment_mask_json, fragment_lit_json::text as fragment_lit_json
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("child_id", request.childId)
            .param("week_id", request.weekId.trim())
            .param("title", request.title.trim())
            .param("note", request.note?.trim())
            .param("image_media_id", request.imageMediaId)
            .param("required_fragments", request.requiredFragments)
            .param("reward_mode", request.rewardMode)
            .param("fragment_visual_mode", request.fragmentVisualMode)
            .param("fragment_grid_rows", fragmentGrid.first)
            .param("fragment_grid_cols", fragmentGrid.second)
            .param("fragment_mask_json", objectMapper.writeValueAsString(fragmentMask))
            .param("fragment_lit_json", objectMapper.writeValueAsString(fragmentLit))
            .param("created_by", user.userId)
            .query(::wishRecord)
            .single()

        imageMedia?.let { imageSuggestionService.upsertProfileForWish(wish, it) }

        eventPublisher.publishFamilyEvent(
            familyId = wish.familyId,
            eventType = "wish.created",
            aggregateType = "wish",
            aggregateId = wish.id,
            payloadJson = wishEventPayload(wish),
        )
        idempotencyService.remember(wish.familyId, idempotencyKey, CREATE_WISH_OPERATION, "wish", wish.id, user.userId)
        return toResponse(wish)
    }

    @Transactional
    fun activateWish(wishId: UUID, idempotencyKey: String?): WishResponse {
        val user = currentUser.require()
        val wish = findWishForUpdate(wishId) ?: throw NotFoundError("Wish not found.")
        familyPolicy.requireParent(user, wish.familyId)
        idempotencyService.find(wish.familyId, idempotencyKey, ACTIVATE_WISH_OPERATION)
            ?.let { return getWish(it.resourceId) }
        if (wish.status !in setOf("draft", "active")) throw ConflictError("Only draft or active wishes can be activated.")
        if (wish.status == "active") return toResponse(wish)

        val activated = try {
            jdbcClient.sql(
                """
                update wish
                set status = 'active',
                    updated_at = now()
                where id = :id
                returning id, family_id, child_id, week_id, title, note, image_media_id,
                          required_fragments, earned_fragments, reward_mode, status,
                          fragment_visual_mode, fragment_grid_rows, fragment_grid_cols,
                          fragment_mask_json::text as fragment_mask_json, fragment_lit_json::text as fragment_lit_json
                """.trimIndent(),
            )
                .param("id", wishId)
                .query(::wishRecord)
                .single()
        } catch (ex: DuplicateKeyException) {
            throw ConflictError("This child already has an active wish for the week.")
        }

        eventPublisher.publishFamilyEvent(
            familyId = activated.familyId,
            eventType = "wish.activated",
            aggregateType = "wish",
            aggregateId = activated.id,
            payloadJson = wishEventPayload(activated),
        )
        idempotencyService.remember(activated.familyId, idempotencyKey, ACTIVATE_WISH_OPERATION, "wish", activated.id, user.userId)
        return toResponse(activated)
    }

    @Transactional
    fun attachWishImage(wishId: UUID, request: AttachWishImageRequest): WishResponse {
        val user = currentUser.require()
        val wish = findWishForUpdate(wishId) ?: throw NotFoundError("Wish not found.")
        familyPolicy.requireParent(user, wish.familyId)
        val media = validateWishImage(request.mediaId, wish.familyId, wish.childId)
        val attached = jdbcClient.sql(
            """
            update wish
            set image_media_id = :image_media_id,
                updated_at = now()
            where id = :id
            returning id, family_id, child_id, week_id, title, note, image_media_id,
                      required_fragments, earned_fragments, reward_mode, status,
                      fragment_visual_mode, fragment_grid_rows, fragment_grid_cols,
                      fragment_mask_json::text as fragment_mask_json, fragment_lit_json::text as fragment_lit_json
            """.trimIndent(),
        )
            .param("id", wish.id)
            .param("image_media_id", media.id)
            .query(::wishRecord)
            .single()

        if (request.sourceType !in setOf("uploaded", "generated", "reused", "imported")) {
            throw BadRequestError("Unsupported wish image sourceType.")
        }
        imageSuggestionService.upsertProfileForWish(attached, media, sourceType = request.sourceType)
        eventPublisher.publishFamilyEvent(
            familyId = attached.familyId,
            eventType = "wish.image_attached",
            aggregateType = "wish",
            aggregateId = attached.id,
            payloadJson = wishEventPayload(attached),
        )
        return toResponse(attached)
    }

    fun getWish(wishId: UUID): WishResponse {
        val wish = findWish(wishId) ?: throw NotFoundError("Wish not found.")
        familyPolicy.requireCanAccessChild(currentUser.require(), wish.childId)
        return toResponse(wish)
    }

    fun getCurrentWish(childId: UUID): WishResponse {
        familyPolicy.requireCanAccessChild(currentUser.require(), childId)
        val wish = jdbcClient.sql(wishSelect("where child_id = :child_id and status in ('active', 'unlocked', 'redeemed') order by updated_at desc limit 1"))
            .param("child_id", childId)
            .query(::wishRecord)
            .optional()
            .orElseThrow { NotFoundError("Current wish not found.") }
        return toResponse(wish)
    }

    fun listChildWishes(childId: UUID, weekId: String?): List<WishResponse> {
        familyPolicy.requireCanAccessChild(currentUser.require(), childId)
        val sql = if (weekId == null) {
            wishSelect("where child_id = :child_id order by created_at desc")
        } else {
            wishSelect("where child_id = :child_id and week_id = :week_id order by created_at desc")
        }
        val spec = jdbcClient.sql(sql).param("child_id", childId)
        if (weekId != null) spec.param("week_id", weekId)
        return spec.query(::wishRecord).list().map(::toResponse)
    }

    fun listChildWishHistory(childId: UUID, limit: Int = 24): List<WishHistoryItemResponse> {
        familyPolicy.requireCanAccessChild(currentUser.require(), childId)
        val pageSize = limit.coerceIn(1, 50)
        val wishes = jdbcClient.sql(wishSelect("where child_id = :child_id order by created_at desc limit :limit"))
            .param("child_id", childId)
            .param("limit", pageSize)
            .query(::wishRecord)
            .list()
        val realizedCounts = realizedCountsByTitle(childId)
        return wishes.map { wish ->
            WishHistoryItemResponse(
                wish = toResponse(wish),
                redemption = findRedemptionByWish(wish.id)?.let(::toRedemptionResponse),
                realizedCount = realizedCounts[wish.title.trim().lowercase()] ?: 0,
            )
        }
    }

    @Transactional
    fun redeemWish(wishId: UUID, request: RedeemWishRequest, idempotencyKey: String?): WishRedemptionResponse {
        val user = currentUser.require()
        validateRedeemRequest(request)
        val wish = findWishForUpdate(wishId) ?: throw NotFoundError("Wish not found.")
        familyPolicy.requireParent(user, wish.familyId)
        idempotencyService.find(wish.familyId, idempotencyKey, REDEEM_WISH_OPERATION)
            ?.let { return getRedemption(it.resourceId) }
        if (wish.status !in setOf("unlocked", "redeemed")) throw ConflictError("Only unlocked wishes can be redeemed.")

        val existing = findRedemptionByWish(wishId)
        if (existing != null) return toRedemptionResponse(existing)
        val photos = request.photoMediaIds.distinct().map { validateRedemptionPhoto(it, wish) }
        if (photos.size != request.photoMediaIds.size) throw BadRequestError("photoMediaIds cannot contain duplicates.")

        val redemption = jdbcClient.sql(
            """
            insert into wish_redemption (
              family_id, child_id, wish_id, redeemed_by, redeemed_date, parent_note, child_note
            ) values (
              :family_id, :child_id, :wish_id, :redeemed_by, :redeemed_date, :parent_note, :child_note
            )
            returning id, family_id, child_id, wish_id, redeemed_date, parent_note, child_note, created_at
            """.trimIndent(),
        )
            .param("family_id", wish.familyId)
            .param("child_id", wish.childId)
            .param("wish_id", wish.id)
            .param("redeemed_by", user.userId)
            .param("redeemed_date", request.redeemedDate)
            .param("parent_note", request.parentNote?.trim())
            .param("child_note", request.childNote?.trim())
            .query(::wishRedemptionRecord)
            .single()

        photos.forEachIndexed { index, media ->
            jdbcClient.sql(
                """
                insert into wish_redemption_media (wish_redemption_id, media_asset_id, sort_order)
                values (:wish_redemption_id, :media_asset_id, :sort_order)
                """.trimIndent(),
            )
                .param("wish_redemption_id", redemption.id)
                .param("media_asset_id", media.id)
                .param("sort_order", index)
                .update()
        }
        jdbcClient.sql(
            """
            update wish
            set status = 'redeemed',
                redeemed_at = now(),
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", wish.id)
            .update()

        eventPublisher.publishFamilyEvent(
            familyId = wish.familyId,
            eventType = "wish.redeemed",
            aggregateType = "wish_redemption",
            aggregateId = redemption.id,
            payloadJson = """
            {
              "redemptionId": "${redemption.id}",
              "wishId": "${wish.id}",
              "familyId": "${wish.familyId}",
              "childId": "${wish.childId}",
              "redeemedDate": "${redemption.redeemedDate}"
            }
            """.trimIndent(),
        )
        idempotencyService.remember(wish.familyId, idempotencyKey, REDEEM_WISH_OPERATION, "wish_redemption", redemption.id, user.userId)
        return toRedemptionResponse(redemption)
    }

    private fun validateCreateWish(request: CreateWishRequest) {
        if (request.title.isBlank()) throw BadRequestError("title cannot be blank.")
        if (request.weekId.isBlank()) throw BadRequestError("weekId cannot be blank.")
        if (request.requiredFragments <= 0) throw BadRequestError("requiredFragments must be positive.")
        if (request.rewardMode !in setOf("flexible", "strict")) throw BadRequestError("Unsupported rewardMode.")
        if (request.fragmentVisualMode !in setOf("grid_reveal", "puzzle_lines", "irregular")) throw BadRequestError("Unsupported fragmentVisualMode.")
        if (request.fragmentGridRows != null && request.fragmentGridRows <= 0) throw BadRequestError("fragmentGridRows must be positive.")
        if (request.fragmentGridCols != null && request.fragmentGridCols <= 0) throw BadRequestError("fragmentGridCols must be positive.")
        if (request.fragmentGridRows != null && request.fragmentGridCols != null && request.fragmentGridRows * request.fragmentGridCols != request.requiredFragments) {
            throw BadRequestError("fragmentGridRows * fragmentGridCols must equal requiredFragments.")
        }
    }

    private fun validateRedeemRequest(request: RedeemWishRequest) {
        if (request.photoMediaIds.size !in 1..3) throw BadRequestError("photoMediaIds must contain 1 to 3 media assets.")
        if (request.parentNote != null && request.parentNote.length > 1000) throw BadRequestError("parentNote is too long.")
        if (request.childNote != null && request.childNote.length > 1000) throw BadRequestError("childNote is too long.")
    }

    private fun ensureChildBelongsToFamily(childId: UUID, familyId: UUID) {
        val count = jdbcClient.sql("select count(*) from child_profile where id = :child_id and family_id = :family_id and status = 'active'")
            .param("child_id", childId)
            .param("family_id", familyId)
            .query(Int::class.java)
            .single()
        if (count != 1) throw BadRequestError("Child profile does not belong to this family.")
    }

    private fun validateWishImage(mediaId: UUID, familyId: UUID, childId: UUID): MediaAssetRecord {
        val media = mediaService.findMedia(mediaId) ?: throw NotFoundError("Wish image media not found.")
        if (media.familyId != familyId || media.childId != childId) throw ForbiddenError("Wish image media does not belong to this child.")
        if (media.purpose != "wish_image") throw BadRequestError("Wish image media must use wish_image purpose.")
        if (media.status !in setOf("uploaded", "ready")) throw ConflictError("Wish image media must be finalized.")
        if (!media.contentType.substringBefore(";").lowercase().startsWith("image/")) throw BadRequestError("Wish image must be an image media asset.")
        return media
    }

    private fun validateRedemptionPhoto(mediaId: UUID, wish: WishRecord): MediaAssetRecord {
        val media = mediaService.findMedia(mediaId) ?: throw NotFoundError("Redemption photo media not found.")
        if (media.familyId != wish.familyId || media.childId != wish.childId) throw ForbiddenError("Redemption photo media does not belong to this wish.")
        if (media.purpose != "wish_redemption") throw BadRequestError("Redemption photos must use wish_redemption purpose.")
        if (media.status !in setOf("uploaded", "ready")) throw ConflictError("Redemption photo media must be finalized.")
        if (!media.contentType.substringBefore(";").lowercase().startsWith("image/")) throw BadRequestError("Redemption photos must be image media assets.")
        return media
    }

    private fun toResponse(wish: WishRecord): WishResponse =
        WishResponse(
            id = wish.id,
            familyId = wish.familyId,
            childId = wish.childId,
            weekId = wish.weekId,
            title = wish.title,
            note = wish.note,
            imageMedia = wish.imageMediaId?.let { mediaId -> mediaService.findMedia(mediaId)?.let(mediaService::toResponse) },
            requiredFragments = wish.requiredFragments,
            earnedFragments = wish.earnedFragments,
            rewardMode = wish.rewardMode,
            status = wish.status,
            fragmentVisual = fragmentVisual(wish),
        )

    private fun toRedemptionResponse(redemption: WishRedemptionRecord): WishRedemptionResponse =
        WishRedemptionResponse(
            id = redemption.id,
            wishId = redemption.wishId,
            redeemedDate = redemption.redeemedDate,
            parentNote = redemption.parentNote,
            childNote = redemption.childNote,
            photos = listRedemptionPhotos(redemption.id).map(mediaService::toResponse),
            createdAt = redemption.createdAt,
        )

    private fun findWish(wishId: UUID): WishRecord? =
        jdbcClient.sql(wishSelect("where id = :id"))
            .param("id", wishId)
            .query(::wishRecord)
            .optional()
            .orElse(null)

    private fun findWishForUpdate(wishId: UUID): WishRecord? =
        jdbcClient.sql("select id from wish where id = :id for update")
            .param("id", wishId)
            .query(UUID::class.java)
            .optional()
            .orElse(null)
            ?.let { findWish(it) }

    private fun findRedemptionByWish(wishId: UUID): WishRedemptionRecord? =
        jdbcClient.sql(redemptionSelect("where wish_id = :wish_id"))
            .param("wish_id", wishId)
            .query(::wishRedemptionRecord)
            .optional()
            .orElse(null)

    private fun realizedCountsByTitle(childId: UUID): Map<String, Int> =
        jdbcClient.sql(
            """
            select lower(trim(w.title)) as title_key, count(*)::int as realized_count
            from wish w
            join wish_redemption wr on wr.wish_id = w.id
            where w.child_id = :child_id
            group by lower(trim(w.title))
            """.trimIndent(),
        )
            .param("child_id", childId)
            .query { rs, _ -> rs.getString("title_key") to rs.getInt("realized_count") }
            .list()
            .toMap()

    private fun getRedemption(redemptionId: UUID): WishRedemptionResponse =
        toRedemptionResponse(
            jdbcClient.sql(redemptionSelect("where id = :id"))
                .param("id", redemptionId)
                .query(::wishRedemptionRecord)
                .optional()
                .orElseThrow { NotFoundError("Wish redemption not found.") },
        )

    private fun listRedemptionPhotos(redemptionId: UUID): List<MediaAssetRecord> =
        jdbcClient.sql(
            """
            select ma.id, ma.family_id, ma.child_id, ma.purpose, ma.storage_key, ma.content_type, ma.size_bytes,
                   ma.status, ma.related_type, ma.related_id
            from wish_redemption_media wrm
            join media_asset ma on ma.id = wrm.media_asset_id
            where wrm.wish_redemption_id = :redemption_id
            order by wrm.sort_order
            """.trimIndent(),
        )
            .param("redemption_id", redemptionId)
            .query(::mediaAssetRecord)
            .list()

    private fun wishSelect(whereClause: String): String =
        """
        select id, family_id, child_id, week_id, title, note, image_media_id,
               required_fragments, earned_fragments, reward_mode, status,
               fragment_visual_mode, fragment_grid_rows, fragment_grid_cols,
               fragment_mask_json::text as fragment_mask_json, fragment_lit_json::text as fragment_lit_json
        from wish
        $whereClause
        """.trimIndent()

    private fun fragmentVisual(wish: WishRecord): WishFragmentVisualResponse {
        val derived = deriveFragmentGrid(wish.requiredFragments)
        val storedRows = wish.fragmentGridRows
        val storedCols = wish.fragmentGridCols
        val fallbackRows = if (storedRows != null && storedCols != null && storedRows * storedCols == wish.requiredFragments) storedRows else derived.first
        val fallbackCols = if (storedRows != null && storedCols != null && storedRows * storedCols == wish.requiredFragments) storedCols else derived.second
        val mask = parseFragmentMask(wish.fragmentMaskJson)
            ?.takeIf { it.total > 0 && it.rows > 0 && it.cols > 0 && it.rows * it.cols == it.total }
            ?: buildFragmentMask(wish.fragmentVisualMode, fallbackRows, fallbackCols)
        val total = mask.total
        val revealed = wish.earnedFragments.coerceIn(0, total)
        val litIndexes = parseLitIndexes(wish.fragmentLitJson, total).ifEmpty {
            mask.revealOrder.take(revealed)
        }.filter { it in 0 until total }.distinct()
        return WishFragmentVisualResponse(
            mode = mask.mode,
            rows = mask.rows,
            cols = mask.cols,
            revealed = revealed,
            total = total,
            mask = mask,
            litIndexes = litIndexes,
        )
    }

    private fun deriveFragmentGrid(requiredFragments: Int): Pair<Int, Int> {
        val target = requiredFragments.coerceAtLeast(1)
        var rows = 1
        var cols = target
        var candidate = 1
        while (candidate * candidate <= target) {
            if (target % candidate == 0) {
                rows = candidate
                cols = target / candidate
            }
            candidate += 1
        }
        return rows to cols
    }

    private fun buildFragmentMask(mode: String, rows: Int, cols: Int): WishFragmentMaskResponse {
        val normalizedRows = rows.coerceAtLeast(1)
        val normalizedCols = cols.coerceAtLeast(1)
        val total = normalizedRows * normalizedCols
        val cells = (0 until total).map { index ->
            WishFragmentCellResponse(
                index = index,
                row = index / normalizedCols,
                col = index % normalizedCols,
                polygon = if (mode == "irregular") irregularPolygon(index) else null,
            )
        }
        return WishFragmentMaskResponse(
            version = 1,
            mode = if (mode in setOf("grid_reveal", "puzzle_lines", "irregular")) mode else "grid_reveal",
            rows = normalizedRows,
            cols = normalizedCols,
            total = total,
            revealOrder = (0 until total).toList(),
            cells = cells,
        )
    }

    private fun buildFragmentLit(litIndexes: List<Int>): Map<String, Any> =
        mapOf(
            "version" to 1,
            "litIndexes" to litIndexes.distinct().sorted(),
        )

    private fun parseFragmentMask(fragmentMaskJson: String?): WishFragmentMaskResponse? {
        val root = fragmentMaskJson?.let(::readJsonOrNull) ?: return null
        val rows = positiveInt(root["rows"]) ?: return null
        val cols = positiveInt(root["cols"]) ?: return null
        val total = positiveInt(root["total"]) ?: rows * cols
        val mode = text(root["mode"]).takeIf { it in setOf("grid_reveal", "puzzle_lines", "irregular") } ?: "grid_reveal"
        val revealOrder = intArray(root["revealOrder"]).filter { it in 0 until total }.distinct().ifEmpty { (0 until total).toList() }
        val cells = root["cells"]?.takeIf { it.isArray }?.mapNotNull { cell ->
            val index = nonNegativeInt(cell["index"]) ?: return@mapNotNull null
            WishFragmentCellResponse(
                index = index,
                row = nonNegativeInt(cell["row"]) ?: index / cols,
                col = nonNegativeInt(cell["col"]) ?: index % cols,
                polygon = polygon(cell["polygon"]),
            )
        }?.filter { it.index in 0 until total }?.sortedBy { it.index }.orEmpty()
        return WishFragmentMaskResponse(
            version = positiveInt(root["version"]) ?: 1,
            mode = mode,
            rows = rows,
            cols = cols,
            total = total,
            revealOrder = revealOrder,
            cells = cells.ifEmpty { buildFragmentMask(mode, rows, cols).cells },
        )
    }

    private fun parseLitIndexes(fragmentLitJson: String?, total: Int): List<Int> {
        val root = fragmentLitJson?.let(::readJsonOrNull) ?: return emptyList()
        return intArray(root["litIndexes"]).filter { it in 0 until total }.distinct()
    }

    private fun readJsonOrNull(value: String): JsonNode? =
        try {
            objectMapper.readTree(value)
        } catch (_: Exception) {
            null
        }

    private fun positiveInt(node: JsonNode?): Int? =
        node?.takeIf { it.isNumber }?.intValue()?.takeIf { it > 0 }

    private fun nonNegativeInt(node: JsonNode?): Int? =
        node?.takeIf { it.isNumber }?.intValue()?.takeIf { it >= 0 }

    private fun text(node: JsonNode?): String? =
        node?.takeIf { it.isTextual }?.asText()

    private fun intArray(node: JsonNode?): List<Int> =
        node?.takeIf { it.isArray }?.mapNotNull { value -> value.takeIf { it.isNumber }?.intValue() }.orEmpty()

    private fun polygon(node: JsonNode?): List<WishFragmentPointResponse>? {
        val points = node?.takeIf { it.isArray }?.mapNotNull { point ->
            val x = point["x"]?.takeIf { it.isNumber }?.doubleValue() ?: return@mapNotNull null
            val y = point["y"]?.takeIf { it.isNumber }?.doubleValue() ?: return@mapNotNull null
            WishFragmentPointResponse(x = x.coerceIn(0.0, 1.0), y = y.coerceIn(0.0, 1.0))
        }.orEmpty()
        return points.takeIf { it.size >= 3 }
    }

    private fun irregularPolygon(index: Int): List<WishFragmentPointResponse> =
        when (index % 5) {
            0 -> listOf(point(0.02, 0.08), point(0.88, 0.0), point(1.0, 0.72), point(0.18, 1.0))
            1 -> listOf(point(0.12, 0.0), point(1.0, 0.14), point(0.86, 1.0), point(0.0, 0.84))
            2 -> listOf(point(0.0, 0.0), point(0.78, 0.1), point(1.0, 1.0), point(0.2, 0.88))
            3 -> listOf(point(0.18, 0.06), point(1.0, 0.0), point(0.82, 0.92), point(0.0, 1.0))
            else -> listOf(point(0.0, 0.2), point(0.72, 0.0), point(1.0, 0.8), point(0.24, 1.0))
        }

    private fun point(x: Double, y: Double): WishFragmentPointResponse =
        WishFragmentPointResponse(x = x, y = y)

    private fun redemptionSelect(whereClause: String): String =
        """
        select id, family_id, child_id, wish_id, redeemed_date, parent_note, child_note, created_at
        from wish_redemption
        $whereClause
        """.trimIndent()

    private fun wishEventPayload(wish: WishRecord): String =
        """
        {
          "wishId": "${wish.id}",
          "familyId": "${wish.familyId}",
          "childId": "${wish.childId}",
          "weekId": "${wish.weekId}",
          "title": ${jsonString(wish.title)},
          "requiredFragments": ${wish.requiredFragments},
          "earnedFragments": ${wish.earnedFragments},
          "rewardMode": "${wish.rewardMode}",
          "status": "${wish.status}"
        }
        """.trimIndent()

    private fun jsonString(value: String): String =
        buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }

    private companion object {
        const val CREATE_WISH_OPERATION = "createWish"
        const val ACTIVATE_WISH_OPERATION = "activateWish"
        const val REDEEM_WISH_OPERATION = "redeemWish"
    }
}
