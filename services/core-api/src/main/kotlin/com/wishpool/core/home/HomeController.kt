package com.wishpool.core.home

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class HomeController(
    private val service: HomeService,
) {
    @GetMapping("/children/{childId}/home-context")
    fun getChildHomeContext(
        @PathVariable childId: UUID,
        @RequestParam(required = false) date: LocalDate?,
    ): ChildHomeContextResponse =
        service.getChildHomeContext(childId, date)

    @GetMapping("/families/{familyId}/parent-dashboard")
    fun getParentDashboardContext(
        @PathVariable familyId: UUID,
        @RequestParam(required = false) childId: UUID?,
        @RequestParam(required = false) date: LocalDate?,
    ): ParentDashboardContextResponse =
        service.getParentDashboardContext(familyId, childId, date)
}
