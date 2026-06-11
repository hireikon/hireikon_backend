package com.hireikon.hireikon_backend.dto

import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.database.model.enums.SkillCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateProfileRequest(
    @field:NotBlank(message = "Full name is required")
    @field:Size(max = 100, message = "Full name must be at most 100 characters")
    val fullName: String,

    @field:Size(max = 20, message = "Phone must be at most 20 characters")
    val phone: String? = null,

    @field:Size(max = 100, message = "Location must be at most 100 characters")
    val location: String? = null,

    @field:Size(max = 255, message = "LinkedIn URL must be at most 255 characters")
    val linkedinUrl: String? = null,

    @field:Size(max = 255, message = "GitHub URL must be at most 255 characters")
    val githubUrl: String? = null,

    val summary: String? = null
)
data class CandidateProfileResponse(
    val id: String,
    val userId: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val location: String?,
    val avatarUrl: String?,
    val resumeUrl: String?,
    val linkedinUrl: String?,
    val githubUrl: String?,
    val summary: String?,
    val totalApplications: Int,
    val skills: List<CandidateSkillResponse>,
    val experiences: List<ExperienceResponse>,
    val educations: List<EducationResponse>
)

data class ResumeUploadResponse(
    val resumeUrl: String
)

data class AddSkillRequest(
    @field:NotBlank(message = "Skill name is required")
    @field:Size(max = 50, message = "Skill name must be at most 50 characters")
    val skillName: String,

    val category: SkillCategory = SkillCategory.OTHER,

    @field:NotNull(message = "Proficiency level is required")
    var proficiencyLevel: ProficiencyLevel
)
data class CandidateSkillResponse(
    val id: String,
    val skillId: String,
    val skillName: String,
    val category: SkillCategory,
    val proficiencyLevel: ProficiencyLevel
)

data class ExperienceRequest(
    @field:NotBlank(message = "Company is required")
    @field:Size(max = 100, message = "Company must be at most 100 characters")
    val company: String,

    @field:NotBlank(message = "Title is required")
    @field:Size(max = 150, message = "Title must be at most 150 characters")
    val title: String,

    @field:NotNull(message = "Start date is required")
    @field:PastOrPresent(message = "Start date cannot be in the future")
    var startDate: LocalDate,

    val endDate: LocalDate? = null,    // null = currently working here

    val description: String? = null
)
data class ExperienceResponse(
    val id: String,
    val company: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isCurrent: Boolean,
    val description: String?
)

data class EducationRequest(
    @field:NotBlank(message = "Institution is required")
    @field:Size(max = 150, message = "Institution must be at most 150 characters")
    val institution: String,

    @field:NotBlank(message = "Degree is required")
    @field:Size(max = 100, message = "Degree must be at most 100 characters")
    val degree: String,

    @field:NotBlank(message = "Field of study is required")
    @field:Size(max = 100, message = "Field must be at most 100 characters")
    val field: String,

    val graduationDate: LocalDate? = null
)
data class EducationResponse(
    val id: String,
    val institution: String,
    val degree: String,
    val field: String,
    val graduationDate: LocalDate?
)