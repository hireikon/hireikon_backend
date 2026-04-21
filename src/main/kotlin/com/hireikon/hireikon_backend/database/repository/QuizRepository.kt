package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.QuizEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuizRepository: JpaRepository<QuizEntity, String> {
    fun findByCandidateId(candidateId: String): List<QuizEntity>
    fun findByCandidateIdAndSkillId(candidateId: String, skillId: String): List<QuizEntity>
}