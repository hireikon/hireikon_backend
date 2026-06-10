package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.database.model.CandidateProfileEntity
import com.hireikon.hireikon_backend.database.model.CandidateSkillEntity
import com.hireikon.hireikon_backend.database.model.EducationEntity
import com.hireikon.hireikon_backend.database.model.ExperienceEntity
import com.hireikon.hireikon_backend.database.model.SkillEntity
import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.database.repository.ApplicationRepository
import com.hireikon.hireikon_backend.database.repository.CandidateProfileRepository
import com.hireikon.hireikon_backend.database.repository.CandidateSkillRepository
import com.hireikon.hireikon_backend.database.repository.EducationRepository
import com.hireikon.hireikon_backend.database.repository.ExperienceRepository
import com.hireikon.hireikon_backend.database.repository.SkillRepository
import com.hireikon.hireikon_backend.dto.AddSkillRequest
import com.hireikon.hireikon_backend.dto.CandidateProfileResponse
import com.hireikon.hireikon_backend.dto.CandidateSkillResponse
import com.hireikon.hireikon_backend.dto.EducationRequest
import com.hireikon.hireikon_backend.dto.EducationResponse
import com.hireikon.hireikon_backend.dto.ExperienceRequest
import com.hireikon.hireikon_backend.dto.ExperienceResponse
import com.hireikon.hireikon_backend.dto.ResumeUploadResponse
import com.hireikon.hireikon_backend.dto.UpdateProfileRequest
import com.hireikon.hireikon_backend.shared.BadRequestException
import com.hireikon.hireikon_backend.shared.DuplicateResourceException
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import com.hireikon.hireikon_backend.shared.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class CandidateService(
    private val candidateProfileRepository: CandidateProfileRepository,
    private val skillRepository: SkillRepository,
    private val candidateSkillRepository: CandidateSkillRepository,
    private val experienceRepository: ExperienceRepository,
    private val educationRepository: EducationRepository,
    private val applicationRepository: ApplicationRepository,
    private val resumeStorageService: ResumeStorageService
) {
    fun getProfile(userId: String): CandidateProfileResponse {
        val profile = findProfileByUserId(userId)
        return profile.toResponse()
    }

    @Transactional
    fun updateProfile(userId: String, request: UpdateProfileRequest): CandidateProfileResponse {
        val profile = findProfileByUserId(userId)

        profile.fullName = request.fullName
        profile.phone = request.phone
        profile.location = request.location
        profile.linkedinUrl = request.linkedinUrl
        profile.githubUrl = request.githubUrl
        profile.summary = request.summary

        return candidateProfileRepository.save(profile).toResponse()
    }

    @Transactional
    fun uploadResume(userId: String, file: MultipartFile): ResumeUploadResponse {
        val profile = findProfileByUserId(userId)
        val resumeUrl = resumeStorageService.uploadResume(file, userId)

        profile.resumeUrl = resumeUrl
        candidateProfileRepository.save(profile)

        return ResumeUploadResponse(resumeUrl)
    }

    @Transactional
    fun deleteResume(userId: String) {
        val profile = findProfileByUserId(userId)

        if (profile.resumeUrl == null) throw BadRequestException("No resume found to delete")

        resumeStorageService.deleteResume(userId)
        profile.resumeUrl = null
        candidateProfileRepository.save(profile)
    }

    @Transactional
    fun saveResumeUrl(userId: String, resumeUrl: String) {
        val profile = findProfileByUserId(userId)
        profile.resumeUrl = resumeUrl
        candidateProfileRepository.save(profile)
    }

    @Transactional
    fun saveAvatarUrl(userId: String, avatarUrl: String) {
        val profile = findProfileByUserId(userId)
        profile.avatarUrl = avatarUrl
        candidateProfileRepository.save(profile)
    }

    @Transactional
    fun deleteAvatarUrl(userId: String) {
        val profile = findProfileByUserId(userId)
        profile.avatarUrl = null
        candidateProfileRepository.save(profile)
    }

    @Transactional(noRollbackFor = [Exception::class])
    fun addSkill(userId: String, request: AddSkillRequest): CandidateSkillResponse {
        val profile = findProfileByUserId(userId)

        val skill = skillRepository.findByNameIgnoreCase(request.skillName)
            .orElseGet {
                skillRepository.save(
                    SkillEntity(
                        name = request.skillName.trim(),
                        category = request.category
                    )
                )
            }

        if (candidateSkillRepository.existsByCandidateIdAndSkillId(profile.id, skill.id)) {
            throw DuplicateResourceException("You already have '${skill.name}' in your skills")
        }

        val candidateSkill = candidateSkillRepository.save(
            CandidateSkillEntity(
                candidate = profile,
                skill = skill,
                proficiencyLevel = request.proficiencyLevel
            )
        )

        return candidateSkill.toResponse()
    }

    @Transactional
    fun updateSkill(
        userId: String,
        candidateSkillId: String,
        proficiencyLevel: ProficiencyLevel
    ): CandidateSkillResponse {
        val profile = findProfileByUserId(userId)
        val candidateSkill = findCandidateSkill(candidateSkillId, profile.id)

        candidateSkill.proficiencyLevel = proficiencyLevel
        return candidateSkillRepository.save(candidateSkill).toResponse()
    }

    @Transactional
    fun removeSkill(userId: String, candidateSkillId: String) {
        val profile = findProfileByUserId(userId)
        val candidateSkill = findCandidateSkill(candidateSkillId, profile.id)
        candidateSkillRepository.delete(candidateSkill)
    }

    fun getSkills(userId: String): List<CandidateSkillResponse> {
        val profile = findProfileByUserId(userId)
        return candidateSkillRepository
            .findByCandidateId(profile.id)
            .map { it.toResponse() }
    }

    @Transactional(noRollbackFor = [Exception::class])
    fun addExperience(userId: String, request: ExperienceRequest): ExperienceResponse {
        val profile = findProfileByUserId(userId)

        if (request.endDate != null && request.endDate.isBefore(request.startDate)) {
            throw BadRequestException("End date cannot be before start date")
        }

        val experience = experienceRepository.save(
            ExperienceEntity(
                candidate = profile,
                company = request.company,
                title = request.title,
                startDate = request.startDate,
                endDate = request.endDate,
                description = request.description
            )
        )

        return experience.toResponse()
    }

    @Transactional
    fun updateExperience(
        userId: String,
        experienceId: String,
        request: ExperienceRequest
    ): ExperienceResponse {
        val profile = findProfileByUserId(userId)
        val experience = findExperience(experienceId, profile.id)

        if (request.endDate != null && request.endDate.isBefore(request.startDate)) {
            throw BadRequestException("End date cannot be before start date")
        }

        experience.company = request.company
        experience.title = request.title
        experience.startDate = request.startDate
        experience.endDate = request.endDate
        experience.description = request.description

        return experienceRepository.save(experience).toResponse()
    }

    @Transactional
    fun deleteExperience(userId: String, experienceId: String) {
        val profile = findProfileByUserId(userId)
        val experience = findExperience(experienceId, profile.id)
        experienceRepository.delete(experience)
    }

    fun getExperiences(userId: String): List<ExperienceResponse> {
        val profile = findProfileByUserId(userId)
        return experienceRepository
            .findByCandidateIdOrderByStartDateDesc(profile.id)
            .map { it.toResponse() }
    }

    @Transactional(noRollbackFor = [Exception::class])
    fun addEducation(userId: String, request: EducationRequest): EducationResponse {
        val profile = findProfileByUserId(userId)

        val education = educationRepository.save(
            EducationEntity(
                candidate = profile,
                institution = request.institution,
                degree = request.degree,
                field = request.field,
                graduationDate = request.graduationDate
            )
        )

        return education.toResponse()
    }

    @Transactional
    fun updateEducation(
        userId: String,
        educationId: String,
        request: EducationRequest
    ): EducationResponse {
        val profile = findProfileByUserId(userId)
        val education = findEducation(educationId, profile.id)

        education.institution = request.institution
        education.degree = request.degree
        education.field = request.field
        education.graduationDate = request.graduationDate

        return educationRepository.save(education).toResponse()
    }

    @Transactional
    fun deleteEducation(userId: String, educationId: String) {
        val profile = findProfileByUserId(userId)
        val education = findEducation(educationId, profile.id)
        educationRepository.delete(education)
    }

    fun getEducations(userId: String): List<EducationResponse> {
        val profile = findProfileByUserId(userId)
        return educationRepository
            .findByCandidateIdOrderByGraduationDateDesc(profile.id)
            .map { it.toResponse() }
    }

    private fun findProfileByUserId(userId: String): CandidateProfileEntity =
        candidateProfileRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Candidate profile not found") }

    private fun findCandidateSkill(candidateSkillId: String, profileId: String): CandidateSkillEntity {
        val skill = candidateSkillRepository.findById(candidateSkillId)
            .orElseThrow { ResourceNotFoundException("Skill not found") }
        if (skill.candidate.id != profileId) throw UnauthorizedException()
        return skill
    }

    private fun findExperience(experienceId: String, profileId: String): ExperienceEntity {
        val exp = experienceRepository.findById(experienceId)
            .orElseThrow { ResourceNotFoundException("Experience not found") }
        if (exp.candidate.id != profileId) throw UnauthorizedException()
        return exp
    }

    private fun findEducation(educationId: String, profileId: String): EducationEntity {
        val edu = educationRepository.findById(educationId)
            .orElseThrow { ResourceNotFoundException("Education not found") }
        if (edu.candidate.id != profileId) throw UnauthorizedException()
        return edu
    }

    private fun CandidateProfileEntity.toResponse() = CandidateProfileResponse(
        id = id,
        userId = user.id,
        fullName = fullName,
        phone = phone,
        location = location,
        avatarUrl = avatarUrl,
        resumeUrl = resumeUrl,
        linkedinUrl = linkedinUrl,
        githubUrl = githubUrl,
        summary = summary,
        totalApplications = applicationRepository.countByCandidateId(id),
        skills = candidateSkillRepository.findByCandidateId(id).map { it.toResponse() },
        experiences = experienceRepository.findByCandidateIdOrderByStartDateDesc(id).map { it.toResponse() },
        educations = educationRepository.findByCandidateIdOrderByGraduationDateDesc(id).map { it.toResponse() }
    )

    private fun CandidateSkillEntity.toResponse() = CandidateSkillResponse(
        id = id,
        skillId = skill.id,
        skillName = skill.name,
        category = skill.category,
        proficiencyLevel = proficiencyLevel
    )

    private fun ExperienceEntity.toResponse() = ExperienceResponse(
        id = id,
        company = company,
        title = title,
        startDate = startDate,
        endDate = endDate,
        isCurrent = endDate == null,
        description = description
    )

    private fun EducationEntity.toResponse() = EducationResponse(
        id = id,
        institution = institution,
        degree = degree,
        field = field,
        graduationDate = graduationDate
    )
}