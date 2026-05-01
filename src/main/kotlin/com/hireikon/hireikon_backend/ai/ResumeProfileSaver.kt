package com.hireikon.hireikon_backend.ai

import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.database.model.enums.SkillCategory
import com.hireikon.hireikon_backend.dto.AddSkillRequest
import com.hireikon.hireikon_backend.dto.EducationRequest
import com.hireikon.hireikon_backend.dto.ExperienceRequest
import com.hireikon.hireikon_backend.dto.ParsedEducationDto
import com.hireikon.hireikon_backend.dto.ParsedExperienceDto
import com.hireikon.hireikon_backend.dto.ParsedResumeDto
import com.hireikon.hireikon_backend.dto.ParsedSkillDto
import com.hireikon.hireikon_backend.dto.UpdateProfileRequest
import com.hireikon.hireikon_backend.service.CandidateService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class ResumeProfileSaver(
    private val candidateService: CandidateService
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = [Exception::class])
    fun saveProfile(userId: String, parsed: ParsedResumeDto, resumeUrl: String?) {
        candidateService.updateProfile(
            userId = userId,
            request = UpdateProfileRequest(
                fullName = parsed.fullName,
                phone = parsed.phone,
                location = parsed.location,
                linkedinUrl = parsed.linkedinUrl,
                githubUrl = parsed.githubUrl,
                summary = parsed.summary
            )
        )
        resumeUrl?.let { candidateService.saveResumeUrl(userId, it) }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = [Exception::class])
    fun saveSkillSafely(userId: String, skillDto: ParsedSkillDto) {
        try {
            candidateService.addSkill(
                userId = userId,
                request = AddSkillRequest(
                    skillName = skillDto.name,
                    category = safeCategory(skillDto.category),
                    proficiencyLevel = safeProficiency(skillDto.proficiencyLevel)
                )
            )
        } catch (ex: Exception) { /* Duplicate or invalid — skip silently */ }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = [Exception::class])
    fun saveExperienceSafely(userId: String, expDto: ParsedExperienceDto) {
        try {
            candidateService.addExperience(
                userId = userId,
                request = ExperienceRequest(
                    company = expDto.company ?: "Unknown",
                    title = expDto.title ?: "Unknown",
                    startDate = parseDate(expDto.startDate) ?: LocalDate.now().minusYears(1),
                    endDate = expDto.endDate?.let { parseDate(it) },
                    description = expDto.description
                )
            )
        } catch (ex: Exception) { /* Invalid data — skip silently */ }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = [Exception::class])
    fun saveEducationSafely(userId: String, eduDto: ParsedEducationDto) {
        try {
            candidateService.addEducation(
                userId = userId,
                request = EducationRequest(
                    institution = eduDto.institution ?: "Unknown",
                    degree = eduDto.degree ?: "Unknown",
                    field = eduDto.field ?: "Not specified",
                    graduationDate = eduDto.graduationDate?.let { parseDate(it) }
                )
            )
        } catch (ex: Exception) { /* Invalid data — skip silently */ }
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy")
        )
        for (formatter in formatters) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter)
            } catch (ex: DateTimeParseException) {
                continue
            }
        }
        return null
    }

    private fun safeCategory(value: String?): SkillCategory {
        return try {
            SkillCategory.valueOf((value ?: "OTHER").uppercase())
        } catch (ex: Exception) {
            SkillCategory.OTHER
        }
    }

    private fun safeProficiency(value: String?): ProficiencyLevel {
        return try {
            ProficiencyLevel.valueOf((value ?: "INTERMEDIATE").uppercase())
        } catch (ex: Exception) {
            ProficiencyLevel.INTERMEDIATE
        }
    }
}