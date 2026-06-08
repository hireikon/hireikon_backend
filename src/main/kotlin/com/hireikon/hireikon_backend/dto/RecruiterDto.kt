package com.hireikon.hireikon_backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateRecruiterProfileRequest(
    @field:NotBlank(message = "Full name is required")
    @field:Size(max = 100, message = "Full name must be at most 100 characters")
    val fullName: String,

    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 100, message = "Company name must be at most 100 characters")
    val companyName: String,

    @field:NotBlank(message = "Position is required")
    @field:Size(max = 150, message = "Position must be at most 150 characters")
    val position: String,

    @field:Size(max = 255, message = "Company website must be at most 255 characters")
    val companyWebsite: String? = null,

    @field:Size(max = 255, message = "LinkedIn URL must be at most 255 characters")
    val linkedinUrl: String? = null,

    @field:Size(max = 100, message = "Location must be at most 100 characters")
    val location: String? = null,

    val bio: String? = null
)

data class RecruiterProfileResponse(
    val id: String,
    val userId: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String?,
    val companyName: String,
    val position: String,
    val companyWebsite: String?,
    val linkedinUrl: String?,
    val location: String?,
    val bio: String?,
    val totalJobsPosted: Int,
    val totalOpenJobs: Int
)