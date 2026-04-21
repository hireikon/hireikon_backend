package com.hireikon.hireikon_backend.dto

import com.hireikon.hireikon_backend.database.model.enums.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

// Register

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Must be a valid email")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{9,}$",
        message = "Password must be at least 9 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character."
    )
    val password: String,

    @field:NotBlank(message = "Full name is required")
    val fullName: String,

    val role: UserRole = UserRole.CANDIDATE,   // defaults to candidate

    // Required only when role = RECRUITER
    val companyName: String? = null,
    val position: String? = null
)

// Login

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Must be a valid email")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

// Refresh Token

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

// Responses

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val user: UserSummary
)

data class UserSummary(
    val id: String,
    val email: String,
    val role: UserRole,
    val fullName: String
)