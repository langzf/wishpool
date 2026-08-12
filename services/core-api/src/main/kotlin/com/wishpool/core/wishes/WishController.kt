package com.wishpool.core.wishes

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class WishController(
    private val service: WishService,
) {
    @PostMapping("/wishes")
    @ResponseStatus(HttpStatus.CREATED)
    fun createWish(
        @Valid @RequestBody request: CreateWishRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): WishResponse =
        service.createWish(request, idempotencyKey)

    @PostMapping("/wishes/{wishId}/activate")
    fun activateWish(
        @PathVariable wishId: UUID,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): WishResponse =
        service.activateWish(wishId, idempotencyKey)

    @GetMapping("/wishes/{wishId}")
    fun getWish(@PathVariable wishId: UUID): WishResponse =
        service.getWish(wishId)

    @PostMapping("/wishes/{wishId}/redeem")
    fun redeemWish(
        @PathVariable wishId: UUID,
        @Valid @RequestBody request: RedeemWishRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): WishRedemptionResponse =
        service.redeemWish(wishId, request, idempotencyKey)

    @GetMapping("/children/{childId}/wishes/current")
    fun getCurrentWish(@PathVariable childId: UUID): WishResponse =
        service.getCurrentWish(childId)

    @GetMapping("/children/{childId}/wishes")
    fun listChildWishes(
        @PathVariable childId: UUID,
        @RequestParam(required = false) weekId: String?,
    ): List<WishResponse> =
        service.listChildWishes(childId, weekId)
}
