package com.hireikon.hireikon_backend.dto

import com.hireikon.hireikon_backend.database.model.enums.ApplicationStatus
import java.time.LocalDateTime

// Candidate applies to a job — no body needed, jobId is in the path
data class ApplicationResponse(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val company: String,
    val candidateId: String,
    val candidateName: String,
    val matchScore: Int?,
    val status: ApplicationStatus,
    val appliedAt: LocalDateTime
)

// Recruiter dashboard — full applicant detail per application
data class ApplicantResponse(
    val applicationId: String,
    val jobId: String,
    val jobTitle: String,
    val candidateId: String,
    val candidateName: String,
    val candidateEmail: String,
    val avatarUrl: String?,
    val resumeUrl: String?,
    val linkedinUrl: String?,
    val githubUrl: String?,
    val matchScore: Int?,
    val status: ApplicationStatus,
    val appliedAt: LocalDateTime,
    val skills: List<CandidateSkillResponse>
)

// Recruiter updates application status
data class UpdateApplicationStatusRequest(
    val status: ApplicationStatus
)

// Candidate's application list item
data class MyApplicationResponse(
    val applicationId: String,
    val job: JobSummaryResponse,
    val matchScore: Int?,
    val status: ApplicationStatus,
    val appliedAt: LocalDateTime
)