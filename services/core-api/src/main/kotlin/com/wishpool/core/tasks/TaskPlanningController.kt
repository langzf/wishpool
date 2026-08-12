package com.wishpool.core.tasks

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class TaskPlanningController(
    private val service: TaskPlanningService,
) {
    @GetMapping("/task-templates")
    fun listTaskTemplates(@RequestParam familyId: UUID): List<TaskTemplateResponse> =
        service.listTaskTemplates(familyId)

    @PostMapping("/task-templates")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTaskTemplate(@Valid @RequestBody request: CreateTaskTemplateRequest): TaskTemplateResponse =
        service.createTaskTemplate(request)

    @PostMapping("/plans")
    fun saveWeeklyPlan(@Valid @RequestBody request: SaveWeeklyPlanRequest): WeeklyPlanResponse =
        service.saveWeeklyPlan(request)

    @GetMapping("/plans/{planId}")
    fun getWeeklyPlan(@PathVariable planId: UUID): WeeklyPlanResponse =
        service.getWeeklyPlan(planId)

    @GetMapping("/children/{childId}/today")
    fun getToday(
        @PathVariable childId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): TodaySnapshotResponse =
        service.getToday(childId, date)

    @PostMapping("/tasks/{taskId}/skip")
    fun skipTask(
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: SkipTaskRequest,
    ): TaskInstanceResponse =
        service.skipTask(taskId, request)

    @PostMapping("/tasks/{taskId}/postpone")
    fun postponeTask(
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: PostponeTaskRequest,
    ): TaskInstanceResponse =
        service.postponeTask(taskId, request)
}
