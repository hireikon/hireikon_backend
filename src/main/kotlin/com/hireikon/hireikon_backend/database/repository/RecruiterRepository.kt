package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.RecruiterEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RecruiterRepository: JpaRepository<RecruiterEntity, String> {
    fun findByUserId(userId: String): Optional<RecruiterEntity>
}