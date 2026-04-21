package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.CandidateSkillEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CandidateSkillRepository: JpaRepository<CandidateSkillEntity, String> {
    fun findByCandidateId(candidateId: String): List<CandidateSkillEntity>
    fun existsByCandidateIdAndSkillId(candidateId: String, skillId: String): Boolean
    fun deleteByCandidateIdAndSkillId(candidateId: String, skillId: String)
}