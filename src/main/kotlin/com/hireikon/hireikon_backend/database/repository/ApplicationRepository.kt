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

    // Cursor paginated — candidate's own applications
    @Query("""
        SELECT a FROM ApplicationEntity a
        WHERE a.candidate.id = :candidateId
        AND (:cursor IS NULL OR a.appliedAt < (SELECT a2.appliedAt FROM ApplicationEntity a2 WHERE a2.id = :cursor))
        ORDER BY a.appliedAt DESC
        LIMIT :pageSize
    """)
    fun findByCandidateIdCursor(
        candidateId: String,
        cursor: String?,
        pageSize: Int
    ): List<ApplicationEntity>

    // Cursor paginated — recruiter dashboard sorted by match score
    @Query("""
        SELECT a FROM ApplicationEntity a
        WHERE a.job.id = :jobId
        AND (:status IS NULL OR a.status = :status)
        AND (:cursor IS NULL OR a.matchScore < (SELECT a2.matchScore FROM ApplicationEntity a2 WHERE a2.id = :cursor)
             OR (a.matchScore = (SELECT a2.matchScore FROM ApplicationEntity a2 WHERE a2.id = :cursor)
                 AND a.appliedAt < (SELECT a2.appliedAt FROM ApplicationEntity a2 WHERE a2.id = :cursor)))
        ORDER BY a.matchScore DESC NULLS LAST, a.appliedAt DESC
        LIMIT :pageSize
    """)
    fun findByJobIdCursor(
        jobId: String,
        status: ApplicationStatus?,
        cursor: String?,
        pageSize: Int
    ): List<ApplicationEntity>
}