package com.wishpool.core.admin

import com.wishpool.core.internal.InternalAuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminController(
    private val service: AdminService,
    private val internalAuthService: InternalAuthService,
) {
    @GetMapping("/internal/admin/dashboard")
    fun dashboard(@RequestHeader("X-Internal-Token", required = false) internalToken: String?): AdminDashboardResponse {
        internalAuthService.requireToken(internalToken)
        return service.dashboard()
    }

    @GetMapping("/internal/admin/families")
    fun listFamilies(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) status: String?,
    ): List<AdminFamilySummaryResponse> {
        internalAuthService.requireToken(internalToken)
        return service.listFamilies(limit, status)
    }

    @GetMapping("/internal/admin/privacy-requests")
    fun listPrivacyRequests(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) status: String?,
    ): List<AdminPrivacyRequestResponse> {
        internalAuthService.requireToken(internalToken)
        return service.listPrivacyRequests(limit, status)
    }

    @GetMapping("/internal/admin/audit-logs")
    fun listAuditLogs(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @RequestParam(required = false) familyId: UUID?,
        @RequestParam(required = false) action: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<AdminAuditLogResponse> {
        internalAuthService.requireToken(internalToken)
        return service.listAuditLogs(familyId, action, limit)
    }

    @PostMapping("/internal/admin/media-access-grants")
    fun grantMediaAccess(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: AdminMediaAccessGrantRequest,
    ): AdminMediaAccessGrantResponse {
        internalAuthService.requireToken(internalToken)
        return service.grantMediaAccess(request)
    }
}
