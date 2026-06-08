package com.hireikon.hireikon_backend.controller

import com.hireikon.hireikon_backend.dto.RecruiterProfileResponse
import com.hireikon.hireikon_backend.dto.UpdateRecruiterProfileRequest
import com.hireikon.hireikon_backend.service.ImageStorageService
import com.hireikon.hireikon_backend.service.RecruiterService
import com.hireikon.hireikon_backend.shared.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/recruiter")
class RecruiterController(
    private val recruiterService: RecruiterService,
    private val imageStorageService: ImageStorageService
) {

    // GET /api/v1/recruiter/profile
    @GetMapping("/profile")
    fun getProfile(): ResponseEntity<ApiResponse<RecruiterProfileResponse>> =
        ResponseEntity.ok(ApiResponse.ok(recruiterService.getProfile(currentUserId())))

    // PUT /api/v1/recruiter/profile
    @PutMapping("/profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateRecruiterProfileRequest
    ): ResponseEntity<ApiResponse<RecruiterProfileResponse>> =
        ResponseEntity.ok(ApiResponse.ok(
            data = recruiterService.updateProfile(currentUserId(), request),
            message = "Profile updated successfully"
        ))

    // POST /api/v1/recruiter/avatar
    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        val userId    = currentUserId()
        val avatarUrl = imageStorageService.uploadProfilePhoto(file, userId, "recruiter")
        recruiterService.saveAvatarUrl(userId, avatarUrl)
        return ResponseEntity.ok(ApiResponse.ok(
            mapOf("avatarUrl" to avatarUrl),
            "Profile photo uploaded successfully"
        ))
    }

    // DELETE /api/v1/recruiter/avatar
    @DeleteMapping("/avatar")
    fun deleteAvatar(): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = currentUserId()
        imageStorageService.deleteProfilePhoto(userId, "recruiter")
        recruiterService.deleteAvatarUrl(userId)
        return ResponseEntity.ok(ApiResponse.ok(null, "Profile photo deleted successfully"))
    }

    private fun currentUserId(): String =
        SecurityContextHolder.getContext().authentication.principal as String
}