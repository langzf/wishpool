package com.wishpool.core.rewards

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class RewardSummaryController(private val service: RewardSummaryService) {
    @GetMapping("/children/{childId}/rewards/summary")
    fun getSummary(
        @PathVariable childId: UUID,
        @RequestParam(required = false) weekId: String?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
    ): RewardSummaryResponse = service.getSummary(childId, weekId, fromDate, toDate)
}
