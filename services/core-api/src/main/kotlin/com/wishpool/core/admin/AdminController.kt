package com.wishpool.core.admin

import com.wishpool.core.internal.InternalAuthService
import com.wishpool.core.ai.ImageGenUsageResponse
import com.wishpool.core.ai.ImageGenUsageWriteRequest
import com.wishpool.core.ai.ImageModelProviderResponse
import com.wishpool.core.ai.ImageModelProviderService
import com.wishpool.core.ai.ImageModelProviderToggleRequest
import com.wishpool.core.ai.ImageModelProviderWriteRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminController(
    private val service: AdminService,
    private val imageModelProviderService: ImageModelProviderService,
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

    @GetMapping("/internal/admin/image-model-providers")
    fun listImageModelProviders(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
    ): List<ImageModelProviderResponse> {
        internalAuthService.requireToken(internalToken)
        return imageModelProviderService.listAdminProviders()
    }

    @PostMapping("/internal/admin/image-model-providers")
    fun createImageModelProvider(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @Valid @RequestBody request: ImageModelProviderWriteRequest,
    ): ImageModelProviderResponse {
        internalAuthService.requireToken(internalToken)
        return imageModelProviderService.createProvider(request)
    }

    @PutMapping("/internal/admin/image-model-providers/{id}")
    fun updateImageModelProvider(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ImageModelProviderWriteRequest,
    ): ImageModelProviderResponse {
        internalAuthService.requireToken(internalToken)
        return imageModelProviderService.updateProvider(id, request)
    }

    @PostMapping("/internal/admin/image-model-providers/{id}/toggle")
    fun toggleImageModelProvider(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ImageModelProviderToggleRequest,
    ): ImageModelProviderResponse {
        internalAuthService.requireToken(internalToken)
        return imageModelProviderService.toggleProvider(id, request)
    }

    @PostMapping("/internal/admin/image-model-providers/{id}/set-default")
    fun setDefaultImageModelProvider(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable id: UUID,
    ): ImageModelProviderResponse {
        internalAuthService.requireToken(internalToken)
        return imageModelProviderService.setDefaultProvider(id)
    }

    @DeleteMapping("/internal/admin/image-model-providers/{id}")
    fun deleteImageModelProvider(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable id: UUID,
    ) {
        internalAuthService.requireToken(internalToken)
        imageModelProviderService.deleteProvider(id)
    }

    @GetMapping("/internal/admin/image-gen-usages")
    fun listImageGenUsages(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
    ): List<ImageGenUsageResponse> {
        internalAuthService.requireToken(internalToken)
        return imageModelProviderService.listUsageMappings()
    }

    @PutMapping("/internal/admin/image-gen-usages/{usageCode}")
    fun upsertImageGenUsage(
        @RequestHeader("X-Internal-Token", required = false) internalToken: String?,
        @PathVariable usageCode: String,
        @Valid @RequestBody request: ImageGenUsageWriteRequest,
    ): ImageGenUsageResponse {
        internalAuthService.requireToken(internalToken)
        return imageModelProviderService.upsertUsageMapping(request.copy(usageCode = usageCode))
    }
}
