package com.wishpool.core.sync

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SyncController(
    private val syncService: SyncService,
) {
    @GetMapping("/sync/pull")
    fun pullSyncEvents(
        @RequestParam familyId: UUID,
        @RequestParam afterSeq: Long,
        @RequestParam(required = false) limit: Int?,
    ): SyncPullResponse =
        syncService.pullEvents(familyId, afterSeq, limit)
}
