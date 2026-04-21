package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.CandidateProfileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CandidateProfileRepository: JpaRepository<CandidateProfileEntity, String> {
    fun findByUserId(userId: String): Optional<CandidateProfileEntity>
}