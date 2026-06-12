package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.JobEntity
import com.hireikon.hireikon_backend.database.model.enums.JobStatus
import com.hireikon.hireikon_backend.database.model.enums.JobType
import com.hireikon.hireikon_backend.database.model.enums.WorkMode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JobRepository: JpaRepository<JobEntity, String> {
    fun findByRecruiterId(recruiterId: String): List<JobEntity>
    fun findByStatus(status: JobStatus): List<JobEntity>

    @Query("""
        SELECT j FROM JobEntity j
        WHERE j.status = 'OPEN'
        AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
        AND (:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword,  '%')))
        AND (:jobType IS NULL OR j.jobType = :jobType)
        AND (:workMode IS NULL OR j.workMode = :workMode)
        AND (:cursor IS NULL OR j.postedAt < (SELECT j2.postedAt FROM JobEntity j2 WHERE j2.id = :cursor))
        ORDER BY j.postedAt DESC
        LIMIT :pageSize
    """)
    fun searchJobsCursor(
        location: String?,
        keyword: String?,
        jobType: JobType?,
        workMode: WorkMode?,
        cursor: String?,
        pageSize: Int
    ): List<JobEntity>

    @Query("""
        SELECT j FROM JobEntity j
        WHERE j.recruiter.id = :recruiterId
        AND (:cursor IS NULL OR j.postedAt < (SELECT j2.postedAt FROM JobEntity j2 WHERE j2.id = :cursor))
        ORDER BY j.postedAt DESC
        LIMIT :pageSize
    """)
    fun findByRecruiterIdCursor(
        recruiterId: String,
        cursor: String?,
        pageSize: Int
    ): List<JobEntity>
}