package com.wishpool.core.submissions

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SubmissionController(
    private val service: SubmissionService,
) {
    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSubmission(@Valid @RequestBody request: CreateSubmissionRequest): SubmissionResponse =
        service.createSubmission(request)

    @GetMapping("/submissions/{submissionId}")
    fun getSubmission(@PathVariable submissionId: UUID): SubmissionDetailResponse =
        service.getSubmission(submissionId)
}
