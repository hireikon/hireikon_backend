package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.database.model.JobEntity
import com.hireikon.hireikon_backend.database.model.JobRequiredSkillEntity
import com.hireikon.hireikon_backend.database.model.SkillEntity
import com.hireikon.hireikon_backend.database.model.enums.JobStatus
import com.hireikon.hireikon_backend.database.model.enums.SkillCategory
import com.hireikon.hireikon_backend.database.repository.JobRepository
import com.hireikon.hireikon_backend.database.repository.JobRequiredSkillRepository
import com.hireikon.hireikon_backend.database.repository.RecruiterRepository
import com.hireikon.hireikon_backend.database.repository.SkillRepository
import com.hireikon.hireikon_backend.dto.CreateJobRequest
import com.hireikon.hireikon_backend.dto.JobResponse
import com.hireikon.hireikon_backend.dto.JobSkillRequest
import com.hireikon.hireikon_backend.dto.JobSkillResponse
import com.hireikon.hireikon_backend.dto.JobSummaryResponse
import com.hireikon.hireikon_backend.dto.UpdateJobRequest
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import com.hireikon.hireikon_backend.shared.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val recruiterRepository: RecruiterRepository,
    private val skillRepository: SkillRepository,
    private val jobRequiredSkillRepository: JobRequiredSkillRepository
) {

    fun getOpenJobs(keyword: String? = null, location: String? = null): List<JobSummaryResponse> =
        jobRepository.searchJobs(location, keyword).map { it.toSummary() }

    fun getJobById(jobId: String): JobResponse {
        val job = findJob(jobId)
        return job.toResponse()
    }

    @Transactional
    fun createJob(userId: String, request: CreateJobRequest): JobResponse {
        val recruiter = recruiterRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Recruiter profile not found") }

        val job = jobRepository.save(
            JobEntity(
                recruiter = recruiter,
                title = request.title,
                company = request.company,
                location = request.location,
                description = request.description,
                status = JobStatus.OPEN,
                postedAt = LocalDateTime.now(),
                deadline = request.deadline
            )
        )
        saveRequiredSkills(job, request.requiredSkills)

        return job.toResponse()
    }

    @Transactional
    fun updateJob(userId: String, jobId: String, request: UpdateJobRequest): JobResponse {
        val job = findOwnedJob(userId, jobId)

        job.title = request.title
        job.company = request.company
        job.location = request.location
        job.description = request.description
        job.status = request.status
        job.deadline = request.deadline

        jobRequiredSkillRepository.deleteByJobId(job.id)
        jobRequiredSkillRepository.flush()
        saveRequiredSkills(job, request.requiredSkills)

        return jobRepository.save(job).toResponse()
    }

    @Transactional
    fun deleteJob(userId: String, jobId: String) {
        val job = findOwnedJob(userId, jobId)
        jobRepository.delete(job)
    }

    @Transactional
    fun updateJobStatus(userId: String, jobId: String, status: JobStatus): JobResponse {
        val job = findOwnedJob(userId, jobId)
        job.status = status
        return jobRepository.save(job).toResponse()
    }

    fun getMyJobs(userId: String): List<JobSummaryResponse> {
        val recruiter = recruiterRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Recruiter profile not found") }
        return jobRepository.findByRecruiterId(recruiter.id).map { it.toSummary() }
    }

    internal fun findJob(jobId: String): JobEntity =
        jobRepository.findById(jobId)
            .orElseThrow { ResourceNotFoundException("Job not found") }

    private fun findOwnedJob(userId: String, jobId: String): JobEntity {
        val recruiter = recruiterRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Recruiter profile not found") }
        val job = findJob(jobId)
        if (job.recruiter.id != recruiter.id) throw UnauthorizedException()
        return job
    }

    private fun saveRequiredSkills(job: JobEntity, skillRequests: List<JobSkillRequest>) {
        skillRequests.forEach { req ->
            val skill = skillRepository.findByNameIgnoreCase(req.skillName)
                .orElseGet {
                    skillRepository.save(
                        SkillEntity(
                            name = req.skillName.trim(),
                            category = SkillCategory.OTHER
                        )
                    )
                }
            jobRequiredSkillRepository.save(
                JobRequiredSkillEntity(
                    job = job,
                    skill = skill,
                    levelRequired = req.levelRequired,
                    isMandatory = req.isMandatory
                )
            )
        }
    }

    private fun JobEntity.toResponse() = JobResponse(
        id = id,
        recruiterId = recruiter.id,
        title = title,
        company = company,
        location = location,
        description = description,
        status = status,
        postedAt = postedAt,
        deadline = deadline,
        requiredSkills = jobRequiredSkillRepository.findByJobId(id).map { it.toSkillResponse() }
    )

    private fun JobEntity.toSummary() = JobSummaryResponse(
        id = id,
        title = title,
        company = company,
        location = location,
        status = status,
        postedAt = postedAt,
        deadline = deadline,
        requiredSkillCount = jobRequiredSkillRepository.findByJobId(id).size
    )

    private fun JobRequiredSkillEntity.toSkillResponse() = JobSkillResponse(
        skillId = skill.id,
        skillName = skill.name,
        levelRequired = levelRequired,
        isMandatory = isMandatory
    )
}