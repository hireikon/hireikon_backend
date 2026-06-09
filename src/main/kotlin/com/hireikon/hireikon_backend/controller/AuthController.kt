package com.hireikon.hireikon_backend.controller

import com.hireikon.hireikon_backend.dto.AuthResponse
import com.hireikon.hireikon_backend.dto.ForgotPasswordRequest
import com.hireikon.hireikon_backend.dto.LoginRequest
import com.hireikon.hireikon_backend.dto.RefreshTokenRequest
import com.hireikon.hireikon_backend.dto.RegisterRequest
import com.hireikon.hireikon_backend.dto.ResetPasswordRequest
import com.hireikon.hireikon_backend.dto.UserSummary
import com.hireikon.hireikon_backend.security.JwtService
import com.hireikon.hireikon_backend.service.AuthService
import com.hireikon.hireikon_backend.service.ForgotPasswordService
import com.hireikon.hireikon_backend.shared.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
    private val forgotPasswordService: ForgotPasswordService
) {

    // POST /api/v1/auth/register
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(
                response, "Account created successfully"
            ))
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.ok(
            response, "Login successful"
        ))
    }

    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.refresh(request)
        return ResponseEntity.ok(ApiResponse.ok(
            response, "Token refreshed"
        ))
    }

    // POST /api/v1/auth/forgot-password
    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<ApiResponse<Nothing?>> {
        forgotPasswordService.requestResetPassword(request.email)
        return ResponseEntity.ok(ApiResponse.ok(
            data = null,
            message = "If an account with that email exists, a reset link has been sent."
        ))
    }

    // POST /api/v1/auth/reset-password
    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<ApiResponse<Nothing?>> {
        forgotPasswordService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.ok(ApiResponse.ok(
            data = null,
            message = "Password reset successfully. Please log in again."
        ))
    }

    // GET /api/v1/auth/me  — returns current user info from JWT (no DB hit)
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal userId: String,
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ApiResponse<UserSummary>> {
        val token = authHeader.substring(7)
        val summary = UserSummary(
            id = userId,
            email = jwtService.getEmail(token),
            role = jwtService.getRole(token),
            fullName = ""    // fetch from profile endpoint for full details
        )
        return ResponseEntity.ok(ApiResponse.ok(summary))
    }

    // POST /api/v1/auth/logout
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userId: String,
        @RequestHeader("Authorization") authHeader: String,
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.logout(userId, request.refreshToken)
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully."))
    }

    // POST /api/v1/auth/logout-all
    @PostMapping("/logout-all")
    fun logoutAll(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.logoutAll(userId)
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out from all devices."))
    }
}