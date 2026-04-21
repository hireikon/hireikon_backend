package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.JobEntity
import com.hireikon.hireikon_backend.database.model.enums.JobStatus
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
        AND (:keyword  IS NULL OR LOWER(j.title)    LIKE LOWER(CONCAT('%', :keyword,  '%')))
    """)
    fun searchJobs(location: String?, keyword: String?): List<JobEntity>
}