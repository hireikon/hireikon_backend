package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.database.model.RecruiterEntity
import com.hireikon.hireikon_backend.database.model.enums.JobStatus
import com.hireikon.hireikon_backend.database.repository.JobRepository
import com.hireikon.hireikon_backend.database.repository.RecruiterRepository
import com.hireikon.hireikon_backend.dto.RecruiterProfileResponse
import com.hireikon.hireikon_backend.dto.UpdateRecruiterProfileRequest
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecruiterService(
    private val recruiterRepository: RecruiterRepository,
    private val jobRepository: JobRepository
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: String): RecruiterProfileResponse {
        val recruiter = findRecruiterByUserId(userId)
        return recruiter.toResponse()
    }

    @Transactional
    fun updateProfile(userId: String, request: UpdateRecruiterProfileRequest): RecruiterProfileResponse {
        val recruiter = findRecruiterByUserId(userId)

        recruiter.fullName = request.fullName
        recruiter.companyName = request.companyName
        recruiter.position = request.position
        recruiter.companyWebsite = request.companyWebsite
        recruiter.linkedinUrl = request.linkedinUrl
        recruiter.location = request.location
        recruiter.bio = request.bio

        return recruiterRepository.save(recruiter).toResponse()
    }

    @Transactional
    fun saveAvatarUrl(userId: String, avatarUrl: String) {
        val recruiter = findRecruiterByUserId(userId)
        recruiter.avatarUrl = avatarUrl
        recruiterRepository.save(recruiter)
    }

    @Transactional
    fun deleteAvatarUrl(userId: String) {
        val recruiter = findRecruiterByUserId(userId)
        recruiter.avatarUrl = null
        recruiterRepository.save(recruiter)
    }

    private fun findRecruiterByUserId(userId: String) =
        recruiterRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Recruiter profile not found") }

    private fun RecruiterEntity.toResponse(): RecruiterProfileResponse {
        val allJobs = jobRepository.findByRecruiterId(id)
        val totalPosted = allJobs.size
        val totalOpen = allJobs.count { it.status == JobStatus.OPEN }

        return RecruiterProfileResponse(
            id = id,
            userId = user.id,
            email = user.email,
            fullName = fullName,
            avatarUrl = avatarUrl,
            companyName = companyName,
            position = position,
            companyWebsite = companyWebsite,
            linkedinUrl = linkedinUrl,
            location = location,
            bio = bio,
            totalJobsPosted = totalPosted,
            totalOpenJobs = totalOpen
        )
    }
}