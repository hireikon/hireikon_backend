package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.RefreshTokenEntity
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

interface RefreshTokenRepository: JpaRepository<RefreshTokenEntity, String> {
    fun findByUserIdAndHashedToken(userId: String, hashedToken: String): RefreshTokenEntity?
    fun deleteByUserIdAndHashedToken(userId: String, hashedToken: String)

    fun findByUserId(userId: String): RefreshTokenEntity?
    fun deleteByUserId(userId: String)

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
    fun deleteAllExpiredTokens(now: Instant): Int
}

@Component
@EnableScheduling
class RefreshTokenCleanupJob(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun purgeExpiredTokens() {
        val deleted = refreshTokenRepository.deleteAllExpiredTokens(Instant.now())
        println("Purged $deleted expired refresh tokens")
    }
}