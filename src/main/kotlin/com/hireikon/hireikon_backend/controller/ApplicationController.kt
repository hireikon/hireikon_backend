package com.hireikon.hireikon_backend.controller

import com.hireikon.hireikon_backend.database.model.enums.ApplicationStatus
import com.hireikon.hireikon_backend.dto.ApplicantResponse
import com.hireikon.hireikon_backend.dto.ApplicationResponse
import com.hireikon.hireikon_backend.dto.MyApplicationResponse
import com.hireikon.hireikon_backend.dto.UpdateApplicationStatusRequest
import com.hireikon.hireikon_backend.service.ApplicationService
import com.hireikon.hireikon_backend.shared.ApiResponse
import com.hireikon.hireikon_backend.shared.CursorPage
import com.hireikon.hireikon_backend.shared.CursorRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/applications")
class ApplicationController(
    private val applicationService: ApplicationService
) {

    // POST /api/v1/applications/{jobId}
    @PostMapping("/{jobId}")
    fun apply(
        @PathVariable jobId: String
    ): ResponseEntity<ApiResponse<ApplicationResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.created(
                applicationService.apply(currentUserId(), jobId),
                "Application submitted successfully"
            )
        )

    // GET /api/v1/applications/my?cursor=uuid&pageSize=20
    @GetMapping("/my")
    fun getMyApplications(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<ApiResponse<CursorPage<MyApplicationResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(
            applicationService.getMyApplications(currentUserId(), CursorRequest(cursor, pageSize))
        ))

    // GET /api/v1/applications/{id}
    @GetMapping("/{id}")
    fun getApplicationById(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<ApplicationResponse>> =
        ResponseEntity.ok(ApiResponse.ok(
            applicationService.getApplicationById(currentUserId(), id)
        ))

    // GET /api/v1/applications/job/{jobId}?status=SHORTLISTED&cursor=uuid&pageSize=20
    @GetMapping("/job/{jobId}")
    fun getApplicants(
        @PathVariable jobId: String,
        @RequestParam(required = false) status: ApplicationStatus?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<ApiResponse<CursorPage<ApplicantResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(
            applicationService.getApplicants(currentUserId(), jobId, status, CursorRequest(cursor, pageSize))
        ))

    // PATCH /api/v1/applications/{id}/status
    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateApplicationStatusRequest
    ): ResponseEntity<ApiResponse<ApplicationResponse>> =
        ResponseEntity.ok(ApiResponse.ok(
            applicationService.updateApplicationStatus(currentUserId(), id, request),
            "Application status updated"
        ))

    private fun currentUserId(): String =
        SecurityContextHolder.getContext().authentication.principal as String
}