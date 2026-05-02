package com.hireikon.hireikon_backend.dto

import com.hireikon.hireikon_backend.database.model.enums.JobStatus
import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateJobRequest(
    @field:NotBlank(message = "Job title is required")
    @field:Size(max = 150, message = "Title must be at most 150 characters")
    val title: String,

    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 100, message = "Company must be at most 100 characters")
    val company: String,

    @field:Size(max = 100, message = "Location must be at most 100 characters")
    val location: String? = null,

    @field:NotBlank(message = "Job description is required")
    val description: String,

    val deadline: LocalDateTime? = null,

    @field:NotEmpty(message = "At least one required skill must be specified")
    val requiredSkills: List<JobSkillRequest>
)

data class UpdateJobRequest(
    @field:NotBlank(message = "Job title is required")
    @field:Size(max = 150)
    val title: String,

    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 100)
    val company: String,

    @field:Size(max = 100)
    val location: String? = null,

    @field:NotBlank(message = "Job description is required")
    val description: String,

    val status: JobStatus = JobStatus.OPEN,
    val deadline: LocalDateTime? = null,

    @field:NotEmpty(message = "At least one required skill must be specified")
    val requiredSkills: List<JobSkillRequest>
)

data class JobSkillRequest(
    @field:NotBlank(message = "Skill name is required")
    val skillName: String,
    val levelRequired: ProficiencyLevel = ProficiencyLevel.INTERMEDIATE,
    val isMandatory: Boolean = true
)

data class JobResponse(
    val id: String,
    val recruiterId: String,
    val title: String,
    val company: String,
    val location: String?,
    val description: String,
    val status: JobStatus,
    val postedAt: LocalDateTime?,
    val deadline: LocalDateTime?,
    val requiredSkills: List<JobSkillResponse>
)

data class JobSkillResponse(
    val skillId: String,
    val skillName: String,
    val levelRequired: ProficiencyLevel,
    val isMandatory: Boolean
)

data class JobSummaryResponse(
    val id: String,
    val title: String,
    val company: String,
    val location: String?,
    val status: JobStatus,
    val postedAt: LocalDateTime?,
    val deadline: LocalDateTime?,
    val requiredSkillCount: Int
)