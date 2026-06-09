package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.database.model.PasswordResetTokenEntity
import com.hireikon.hireikon_backend.database.repository.CandidateProfileRepository
import com.hireikon.hireikon_backend.database.repository.PasswordResetTokenRepository
import com.hireikon.hireikon_backend.database.repository.RecruiterRepository
import com.hireikon.hireikon_backend.database.repository.RefreshTokenRepository
import com.hireikon.hireikon_backend.database.repository.UserRepository
import com.hireikon.hireikon_backend.shared.BadRequestException
import com.hireikon.hireikon_backend.shared.UnauthorizedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class ForgotPasswordService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val candidateProfileRepository: CandidateProfileRepository,
    private val recruiterRepository: RecruiterRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.password-reset.expiry-minutes:15}") private val expiryMinutes: Long
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun requestResetPassword(email: String) {
        val user = userRepository.findByEmail(email).orElse(null) ?: return

        passwordResetTokenRepository.deleteByUserId(user.id)

        val rawToken = generateSecureToken()
        val hashed = hashToken(rawToken)
        val expiresAt = Instant.now().plusSeconds(expiryMinutes * 60)

        passwordResetTokenRepository.save(
            PasswordResetTokenEntity(
                userId = user.id,
                hashedToken = hashed,
                expiresAt = expiresAt
            )
        )

        val fullName = resolveFullName(user.id, user.role.name)

        try {
            emailService.sendPasswordResetEmail(user.email, rawToken, fullName)
        } catch (ex: Exception) {
            println("Warning: Could not send reset email to ${user.email}: ${ex.message}")
        }
    }

    @Transactional
    fun resetPassword(rawToken: String, newPassword: String) {
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{9,}$")
        if (!passwordRegex.matches(newPassword)) {
            throw BadRequestException("Password must be at least 9 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
        }

        val hashed = hashToken(rawToken)

        val tokenEntity = passwordResetTokenRepository.findByHashedToken(hashed)
            .orElseThrow { UnauthorizedException("Invalid or expired reset token") }

        if (Instant.now().isAfter(tokenEntity.expiresAt)) {
            passwordResetTokenRepository.delete(tokenEntity)
            passwordResetTokenRepository.flush()
            throw UnauthorizedException("Reset token has expired. Please request a new one.")
        }

        val user = userRepository.findById(tokenEntity.userId)
            .orElseThrow { UnauthorizedException("User not found") }

        if (passwordEncoder.matches(newPassword, user.passwordHash)) {
            throw BadRequestException("New password must be different from your current password")
        }

        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        passwordResetTokenRepository.delete(tokenEntity)
        refreshTokenRepository.deleteByUserId(user.id)
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    fun purgeExpiredTokens() {
        val deleted = passwordResetTokenRepository.deleteAllExpired(Instant.now())
        if (deleted > 0) println("Purged $deleted expired password reset tokens")
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(
            digest.digest(token.toByteArray())
        )
    }

    private fun resolveFullName(userId: String, role: String): String = when (role) {
        "CANDIDATE" -> candidateProfileRepository.findByUserId(userId)
            .map { it.fullName }.orElse("there")

        "RECRUITER" -> recruiterRepository.findByUserId(userId)
            .map { it.fullName }.orElse("there")

        else -> "there"
    }
}