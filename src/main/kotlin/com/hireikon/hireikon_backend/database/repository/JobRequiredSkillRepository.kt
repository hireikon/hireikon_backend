package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.JobRequiredSkillEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobRequiredSkillRepository: JpaRepository<JobRequiredSkillEntity, String> {
    fun findByJobId(jobId: String): List<JobRequiredSkillEntity>
    fun deleteByJobIdAndSkillId(jobId: String, skillId: String)
}