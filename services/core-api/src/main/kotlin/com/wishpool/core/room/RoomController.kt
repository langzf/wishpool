package com.wishpool.core.room

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RoomController(
    private val roomService: RoomService,
) {
    @GetMapping("/room/state")
    fun getRoomState(@RequestParam childId: UUID): RoomStateResponse =
        roomService.getRoomState(childId)

    @PostMapping("/room/items/{itemId}/arrange")
    fun arrangeRoomItem(
        @PathVariable itemId: UUID,
        @Valid @RequestBody request: ArrangeRoomItemRequest,
    ): RoomItemResponse =
        roomService.arrangeRoomItem(itemId, request)
}
