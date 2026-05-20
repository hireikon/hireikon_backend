package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.QuizEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface QuizRepository: JpaRepository<QuizEntity, String> {
    fun findByCandidateId(candidateId: String): List<QuizEntity>
    fun findByCandidateIdAndSkillId(candidateId: String, skillId: String): List<QuizEntity>

    @Query("""
        SELECT q FROM QuizEntity q
        WHERE q.candidate.id = :candidateId
        AND (:cursor IS NULL OR q.takenAt < (SELECT q2.takenAt FROM QuizEntity q2 WHERE q2.id = :cursor))
        ORDER BY q.takenAt DESC
        LIMIT :pageSize
    """)
    fun findByCandidateIdCursor(
        candidateId: String,
        cursor: String?,
        pageSize: Int
    ): List<QuizEntity>

    @Query("""
        SELECT q FROM QuizEntity q
        WHERE q.candidate.id = :candidateId
        AND q.skill.id = :skillId
        AND (:cursor IS NULL OR q.takenAt < (SELECT q2.takenAt FROM QuizEntity q2 WHERE q2.id = :cursor))
        ORDER BY q.takenAt DESC
        LIMIT :pageSize
    """)
    fun findByCandidateIdAndSkillIdCursor(
        candidateId: String,
        skillId: String,
        cursor: String?,
        pageSize: Int
    ): List<QuizEntity>
}