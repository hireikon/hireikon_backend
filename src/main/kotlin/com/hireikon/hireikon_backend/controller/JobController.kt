package com.hireikon.hireikon_backend.controller

import com.hireikon.hireikon_backend.database.model.enums.JobStatus
import com.hireikon.hireikon_backend.database.model.enums.JobType
import com.hireikon.hireikon_backend.database.model.enums.WorkMode
import com.hireikon.hireikon_backend.dto.CreateJobRequest
import com.hireikon.hireikon_backend.dto.JobResponse
import com.hireikon.hireikon_backend.dto.JobSummaryResponse
import com.hireikon.hireikon_backend.dto.UpdateJobRequest
import com.hireikon.hireikon_backend.service.JobService
import com.hireikon.hireikon_backend.shared.ApiResponse
import com.hireikon.hireikon_backend.shared.CursorPage
import com.hireikon.hireikon_backend.shared.CursorRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/jobs")
class JobController(
    private val jobService: JobService
) {

    // GET /api/v1/jobs?keyword=kotlin&location=dhaka&jobType=FULL_TIME&workMode=ON_SITE&cursor=uuid&pageSize=20
    @GetMapping
    fun getOpenJobs(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) jobType: JobType?,
        @RequestParam(required = false) workMode: WorkMode?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<ApiResponse<CursorPage<JobSummaryResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(
            jobService.getOpenJobs(keyword, location, jobType, workMode, CursorRequest(cursor, pageSize))
        ))

    // GET /api/v1/jobs/{id}
    @GetMapping("/{id}")
    fun getJobById(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<JobResponse>> =
        ResponseEntity.ok(ApiResponse.ok(jobService.getJobById(id)))

    // GET /api/v1/jobs/my
    // GET /api/v1/jobs/my?cursor=uuid&pageSize=20
    @GetMapping("/my")
    fun getMyJobs(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<ApiResponse<CursorPage<JobSummaryResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(
            jobService.getMyJobs(currentUserId(), CursorRequest(cursor, pageSize))
        ))

    // POST /api/v1/jobs
    @PostMapping
    fun createJob(
        @Valid @RequestBody request: CreateJobRequest
    ): ResponseEntity<ApiResponse<JobResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.created(
                jobService.createJob(currentUserId(), request),
                "Job posted successfully"
            )
        )

    // PUT /api/v1/jobs/{id}
    @PutMapping("/{id}")
    fun updateJob(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateJobRequest
    ): ResponseEntity<ApiResponse<JobResponse>> =
        ResponseEntity.ok(ApiResponse.ok(
            jobService.updateJob(currentUserId(), id, request),
            "Job updated successfully"
        ))

    // PATCH /api/v1/jobs/{id}/status?status=CLOSED
    @PatchMapping("/{id}/status")
    fun updateJobStatus(
        @PathVariable id: String,
        @RequestParam status: JobStatus
    ): ResponseEntity<ApiResponse<JobResponse>> =
        ResponseEntity.ok(ApiResponse.ok(
            jobService.updateJobStatus(currentUserId(), id, status),
            "Job status updated"
        ))

    // DELETE /api/v1/jobs/{id}
    @DeleteMapping("/{id}")
    fun deleteJob(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        jobService.deleteJob(currentUserId(), id)
        return ResponseEntity.ok(ApiResponse.ok(null, "Job deleted successfully"))
    }

    private fun currentUserId(): String =
        SecurityContextHolder.getContext().authentication.principal as String
}