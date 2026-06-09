package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.PasswordResetTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface PasswordResetTokenRepository: JpaRepository<PasswordResetTokenEntity, String> {
    fun findByHashedToken(hashedToken: String): Optional<PasswordResetTokenEntity>

    @Modifying
    @Query("""
        DELETE FROM PasswordResetTokenEntity t
        WHERE t.userId = :userId
    """)
    fun deleteByUserId(userId: String)

    @Modifying
    @Query("""
        DELETE FROM PasswordResetTokenEntity t
        WHERE t.expiresAt < :now
    """)
    fun deleteAllExpired(now: Instant): Int
}