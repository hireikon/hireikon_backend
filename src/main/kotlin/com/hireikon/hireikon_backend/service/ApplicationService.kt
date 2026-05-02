package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.ai.SkillGapAnalyzer
import com.hireikon.hireikon_backend.database.model.ApplicationEntity
import com.hireikon.hireikon_backend.database.model.SkillGapReportEntity
import com.hireikon.hireikon_backend.database.model.enums.ApplicationStatus
import com.hireikon.hireikon_backend.database.model.enums.JobStatus
import com.hireikon.hireikon_backend.database.repository.ApplicationRepository
import com.hireikon.hireikon_backend.database.repository.CandidateProfileRepository
import com.hireikon.hireikon_backend.database.repository.JobRequiredSkillRepository
import com.hireikon.hireikon_backend.database.repository.SkillGapReportRepository
import com.hireikon.hireikon_backend.dto.ApplicantResponse
import com.hireikon.hireikon_backend.dto.ApplicationResponse
import com.hireikon.hireikon_backend.dto.JobSummaryResponse
import com.hireikon.hireikon_backend.dto.MyApplicationResponse
import com.hireikon.hireikon_backend.dto.UpdateApplicationStatusRequest
import com.hireikon.hireikon_backend.shared.BadRequestException
import com.hireikon.hireikon_backend.shared.DuplicateResourceException
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import com.hireikon.hireikon_backend.shared.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val candidateProfileRepository: CandidateProfileRepository,
    private val skillGapReportRepository: SkillGapReportRepository,
    private val jobRequiredSkillRepository: JobRequiredSkillRepository,
    private val jobService: JobService,
    private val skillGapAnalyzer: SkillGapAnalyzer
) {

    @Transactional
    fun apply(userId: String, jobId: String): ApplicationResponse {
        val profile = candidateProfileRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Candidate profile not found") }

        val job = jobService.findJob(jobId)

        if (job.status != JobStatus.OPEN) {
            throw BadRequestException("This job is no longer accepting applications")
        }

        if (applicationRepository.existsByCandidateIdAndJobId(profile.id, jobId)) {
            throw DuplicateResourceException("You have already applied for this job")
        }

        val matchScore = skillGapAnalyzer.calculateLocalMatchScore(userId, jobId)
        val application = applicationRepository.save(
            ApplicationEntity(
                candidate = profile,
                job = job,
                matchScore = matchScore,
                status = ApplicationStatus.PENDING
            )
        )

        // Generate and store the full AI skill gap report asynchronously
        // stored as JSON in skill_gap_reports for recruiter and candidate to view later
        try {
            val report = skillGapAnalyzer.analyze(userId, jobId)
            skillGapReportRepository.save(
                SkillGapReportEntity(
                    application = application,
                    missingSkills = skillGapAnalyzer.serializeMissingSkills(report),
                    learningRoadmap = skillGapAnalyzer.serializeLearningRoadmap(report)
                )
            )
        } catch (ex: Exception) {
            // Non-fatal — don't fail the application if AI analysis fails
            println("Warning: Could not generate skill gap report for application ${application.id}: ${ex.message}")
        }

        return application.toResponse()
    }

    @Transactional(readOnly = true)
    fun getMyApplications(userId: String): List<MyApplicationResponse> {
        val profile = candidateProfileRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Candidate profile not found") }
        return applicationRepository.findByCandidateId(profile.id)
            .map { it.toMyResponse() }
    }

    @Transactional(readOnly = true)
    fun getApplicationById(userId: String, applicationId: String): ApplicationResponse {
        val profile = candidateProfileRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Candidate profile not found") }
        val application = findApplication(applicationId)
        if (application.candidate.id != profile.id) throw UnauthorizedException()
        return application.toResponse()
    }

    @Transactional(readOnly = true)
    fun getApplicants(
        userId: String,
        jobId: String,
        status: ApplicationStatus? = null
    ): List<ApplicantResponse> {
        val job = jobService.findJob(jobId)
        val recruiter = job.recruiter
        if (recruiter.user.id != userId) throw UnauthorizedException()

        val applications = when {
            status != null -> applicationRepository.findByJobIdAndStatus(jobId, status)
            else -> applicationRepository.findByJobIdOrderByMatchScoreDesc(jobId)
        }

        return applications.map { it.toApplicantResponse() }
    }

    @Transactional
    fun updateApplicationStatus(
        userId: String,
        applicationId: String,
        request: UpdateApplicationStatusRequest
    ): ApplicationResponse {
        val application = findApplication(applicationId)
        val recruiter = application.job.recruiter
        if (recruiter.user.id != userId) throw UnauthorizedException()

        application.status = request.status
        return applicationRepository.save(application).toResponse()
    }

    private fun findApplication(applicationId: String): ApplicationEntity =
        applicationRepository.findById(applicationId)
            .orElseThrow { ResourceNotFoundException("Application not found") }

    private fun ApplicationEntity.toResponse() = ApplicationResponse(
        id = id,
        jobId = job.id,
        jobTitle = job.title,
        company = job.company,
        candidateId = candidate.id,
        candidateName = candidate.fullName,
        matchScore = matchScore,
        status = status,
        appliedAt = appliedAt
    )

    private fun ApplicationEntity.toMyResponse() = MyApplicationResponse(
        applicationId = id,
        job = JobSummaryResponse(
            id = job.id,
            title = job.title,
            company = job.company,
            location = job.location,
            status = job.status,
            postedAt = job.postedAt,
            deadline = job.deadline,
            requiredSkillCount = jobRequiredSkillRepository.findByJobId(job.id).size
        ),
        matchScore = matchScore,
        status = status,
        appliedAt = appliedAt
    )

    private fun ApplicationEntity.toApplicantResponse() = ApplicantResponse(
        applicationId = id,
        candidateId = candidate.id,
        candidateName = candidate.fullName,
        candidateEmail = candidate.user.email,
        resumeUrl = candidate.resumeUrl,
        linkedinUrl = candidate.linkedinUrl,
        githubUrl = candidate.githubUrl,
        matchScore = matchScore,
        status = status,
        appliedAt = appliedAt
    )
}