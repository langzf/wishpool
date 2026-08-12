package com.wishpool.core.memories

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MemoryController(
    private val memoryService: MemoryService,
) {
    @GetMapping("/memories")
    fun listMemories(
        @RequestParam childId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
    ): MemoryTimelineResponse =
        memoryService.listMemories(childId, cursor, limit)

    @GetMapping("/memories/{memoryId}")
    fun getMemory(@PathVariable memoryId: UUID): WeeklyMemoryResponse =
        memoryService.getMemory(memoryId)

    @PostMapping("/memories/{memoryId}/export")
    fun exportMemory(
        @PathVariable memoryId: UUID,
        @Valid @RequestBody request: ExportMemoryRequest,
    ): MemoryExportResponse =
        memoryService.exportMemory(memoryId, request)
}
