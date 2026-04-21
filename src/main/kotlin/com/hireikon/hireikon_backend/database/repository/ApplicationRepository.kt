package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.ApplicationEntity
import com.hireikon.hireikon_backend.database.model.enums.ApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ApplicationRepository: JpaRepository<ApplicationEntity, String> {
    fun findByCandidateId(candidateId: String): List<ApplicationEntity>
    fun findByJobId(jobId: String): List<ApplicationEntity>
    fun existsByCandidateIdAndJobId(candidateId: String, jobId: String): Boolean

    // Recruiter dashboard — top candidates for a job, sorted by match score
    @Query("""
        SELECT a FROM ApplicationEntity a
        WHERE a.job.id = :jobId
        ORDER BY a.matchScore DESC NULLS LAST
    """)
    fun findByJobIdOrderByMatchScoreDesc(jobId: String): List<ApplicationEntity>

    // Shortlisted only
    fun findByJobIdAndStatus(jobId: String, status: ApplicationStatus): List<ApplicationEntity>
}