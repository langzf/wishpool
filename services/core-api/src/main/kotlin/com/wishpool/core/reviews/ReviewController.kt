package com.wishpool.core.reviews

import com.wishpool.core.submissions.SubmissionDetailResponse
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
class ReviewController(
    private val service: ReviewService,
) {
    @GetMapping("/reviews/pending")
    fun listPendingReviews(@RequestParam familyId: UUID): List<PendingReviewCardResponse> =
        service.listPendingReviews(familyId)

    @GetMapping("/reviews/{submissionId}/detail")
    fun getReviewDetail(@PathVariable submissionId: UUID): SubmissionDetailResponse =
        service.getReviewDetail(submissionId)

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun reviewSubmission(
        @Valid @RequestBody request: ReviewSubmissionRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ReviewResponse =
        service.reviewSubmission(request, idempotencyKey)

    @PostMapping("/reviews/{reviewId}/revoke")
    fun revokeReview(
        @PathVariable reviewId: UUID,
        @Valid @RequestBody request: RevokeReviewRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ReviewResponse =
        service.revokeReview(reviewId, request, idempotencyKey)
}
