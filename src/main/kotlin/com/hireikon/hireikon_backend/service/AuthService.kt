package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.database.model.CandidateProfileEntity
import com.hireikon.hireikon_backend.database.model.RecruiterEntity
import com.hireikon.hireikon_backend.database.model.RefreshTokenEntity
import com.hireikon.hireikon_backend.database.model.UserEntity
import com.hireikon.hireikon_backend.database.model.enums.UserRole
import com.hireikon.hireikon_backend.database.repository.CandidateProfileRepository
import com.hireikon.hireikon_backend.database.repository.RecruiterRepository
import com.hireikon.hireikon_backend.database.repository.RefreshTokenRepository
import com.hireikon.hireikon_backend.database.repository.UserRepository
import com.hireikon.hireikon_backend.dto.AuthResponse
import com.hireikon.hireikon_backend.dto.LoginRequest
import com.hireikon.hireikon_backend.dto.RefreshTokenRequest
import com.hireikon.hireikon_backend.dto.RegisterRequest
import com.hireikon.hireikon_backend.dto.UserSummary
import com.hireikon.hireikon_backend.security.JwtService
import com.hireikon.hireikon_backend.shared.BadRequestException
import com.hireikon.hireikon_backend.shared.DuplicateResourceException
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import com.hireikon.hireikon_backend.shared.UnauthorizedException
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val candidateProfileRepository: CandidateProfileRepository,
    private val recruiterRepository: RecruiterRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("An account with this email already exists.")
        }

        if (request.role == UserRole.RECRUITER) {
            if (request.companyName.isNullOrBlank()) throw BadRequestException("Company name is required for recruiter accounts.")
            if (request.position.isNullOrBlank())   throw BadRequestException("Position is required for recruiter accounts.")
        }

        // 1. Create base user
        val user = userRepository.save(
            UserEntity(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                role = request.role
            )
        )

        // 2. Create role-specific profile
        val fullName = when (request.role) {
            UserRole.CANDIDATE -> {
                val profile = CandidateProfileEntity(user = user, fullName = request.fullName)
                candidateProfileRepository.save(profile)
                request.fullName
            }
            UserRole.RECRUITER -> {
                val recruiter = RecruiterEntity(
                    user = user,
                    fullName = request.fullName,
                    companyName = request.companyName!!,
                    position = request.position!!
                )
                recruiterRepository.save(recruiter)
                request.fullName
            }
            UserRole.ADMIN -> request.fullName
        }

        return buildAuthResponseAndStoreRefreshToken(user, fullName)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BadCredentialsException("Invalid email or password.") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BadCredentialsException("Invalid email or password.")
        }

        val fullName = resolveFullName(user)
        return buildAuthResponseAndStoreRefreshToken(user, fullName)
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val refreshToken = request.refreshToken

        if (!jwtService.validateToken(refreshToken)) {
            throw UnauthorizedException("Invalid or expired refresh token.")
        }
        if (jwtService.isAccessToken(refreshToken)) {
            throw UnauthorizedException("Expected a refresh token, not an access token.")
        }

        val userId = jwtService.getUserId(refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found.") }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw UnauthorizedException("Refresh token not recognized (maybe used or expired).")
        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        val fullName = resolveFullName(user)
        return buildAuthResponseAndStoreRefreshToken(user, fullName)
    }

    @Transactional
    fun logout(userId: String, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        refreshTokenRepository.deleteByUserIdAndHashedToken(userId, hashed)
    }

    @Transactional
    fun logoutAll(userId: String) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    private fun resolveFullName(user: UserEntity): String =
        when (user.role) {
            UserRole.CANDIDATE -> candidateProfileRepository
                .findByUserId(user.id)
                .map { it.fullName }
                .orElse(user.email)
            UserRole.RECRUITER -> recruiterRepository
                .findByUserId(user.id)
                .map { it.fullName }
                .orElse(user.email)
            UserRole.ADMIN -> "Admin"
        }

    private fun buildAuthResponseAndStoreRefreshToken(user: UserEntity, fullName: String): AuthResponse {
        val accessToken  = jwtService.generateAccessToken(user.id, user.email, user.role)
        val refreshToken = jwtService.generateRefreshToken(user.id, user.email, user.role)
        storeRefreshToken(user.id, refreshToken)

        return AuthResponse(
            accessToken  = accessToken,
            refreshToken = refreshToken,
            user = UserSummary(
                id = user.id,
                email = user.email,
                role = user.role,
                fullName = fullName
            )
        )
    }

    private fun storeRefreshToken(userId: String, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenExpirationMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = userId,
                hashedToken = hashed,
                expiresAt = expiresAt
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}