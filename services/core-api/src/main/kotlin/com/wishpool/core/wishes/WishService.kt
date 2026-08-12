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
import java.util.UUID

@Service
class WishService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
    private val eventPublisher: DomainEventPublisher,
    private val idempotencyService: IdempotencyService,
) {
    @Transactional
    fun createWish(request: CreateWishRequest, idempotencyKey: String?): WishResponse {
        val user = currentUser.require()
        familyPolicy.requireParent(user, request.familyId)
        validateCreateWish(request)
        ensureChildBelongsToFamily(request.childId, request.familyId)
        request.imageMediaId?.let { validateWishImage(it, request.familyId, request.childId) }
        idempotencyService.find(request.familyId, idempotencyKey, CREATE_WISH_OPERATION)
            ?.let { return getWish(it.resourceId) }

        val wish = jdbcClient.sql(
            """
            insert into wish (
              family_id, child_id, week_id, title, note, image_media_id,
              required_fragments, reward_mode, status, created_by
            ) values (
              :family_id, :child_id, :week_id, :title, :note, :image_media_id,
              :required_fragments, :reward_mode, 'draft', :created_by
            )
            returning id, family_id, child_id, week_id, title, note, image_media_id,
                      required_fragments, earned_fragments, reward_mode, status
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
            .param("created_by", user.userId)
            .query(::wishRecord)
            .single()

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
                          required_fragments, earned_fragments, reward_mode, status
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

    private fun validateWishImage(mediaId: UUID, familyId: UUID, childId: UUID) {
        val media = mediaService.findMedia(mediaId) ?: throw NotFoundError("Wish image media not found.")
        if (media.familyId != familyId || media.childId != childId) throw ForbiddenError("Wish image media does not belong to this child.")
        if (media.purpose != "wish_image") throw BadRequestError("Wish image media must use wish_image purpose.")
        if (media.status !in setOf("uploaded", "ready")) throw ConflictError("Wish image media must be finalized.")
        if (!media.contentType.substringBefore(";").lowercase().startsWith("image/")) throw BadRequestError("Wish image must be an image media asset.")
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
               required_fragments, earned_fragments, reward_mode, status
        from wish
        $whereClause
        """.trimIndent()

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
