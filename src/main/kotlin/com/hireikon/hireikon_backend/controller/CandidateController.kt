package com.hireikon.hireikon_backend.controller

import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.dto.AddSkillRequest
import com.hireikon.hireikon_backend.dto.CandidateProfileResponse
import com.hireikon.hireikon_backend.dto.CandidateSkillResponse
import com.hireikon.hireikon_backend.dto.EducationRequest
import com.hireikon.hireikon_backend.dto.EducationResponse
import com.hireikon.hireikon_backend.dto.ExperienceRequest
import com.hireikon.hireikon_backend.dto.ExperienceResponse
import com.hireikon.hireikon_backend.dto.ResumeUploadResponse
import com.hireikon.hireikon_backend.dto.UpdateProfileRequest
import com.hireikon.hireikon_backend.service.CandidateService
import com.hireikon.hireikon_backend.shared.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/candidate")
class CandidateController(
    private val candidateService: CandidateService
) {

    // GET /api/v1/candidate/profile
    @GetMapping("/profile")
    fun getProfile(): ResponseEntity<ApiResponse<CandidateProfileResponse>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(candidateService.getProfile(userId)))
    }

    // PUT /api/v1/candidate/profile
    @PutMapping("/profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<ApiResponse<CandidateProfileResponse>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(
            candidateService.updateProfile(userId, request),
            "Profile updated successfully"
        ))
    }

    // POST /api/v1/candidate/resume  (multipart/form-data, field name: "file")
    @PostMapping("/resume", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadResume(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ApiResponse<ResumeUploadResponse>> {
        val userId = currentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
            candidateService.uploadResume(userId, file),
            "Resume uploaded successfully"
        ))
    }

    // DELETE /api/v1/candidate/resume
    @DeleteMapping("/resume")
    fun deleteResume(): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = currentUserId()
        candidateService.deleteResume(userId)
        return ResponseEntity.ok(ApiResponse.ok(null, "Resume deleted successfully"))
    }

    // GET /api/v1/candidate/skills
    @GetMapping("/skills")
    fun getSkills(): ResponseEntity<ApiResponse<List<CandidateSkillResponse>>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(candidateService.getSkills(userId)))
    }

    // POST /api/v1/candidate/skills
    @PostMapping("/skills")
    fun addSkill(
        @Valid @RequestBody request: AddSkillRequest
    ): ResponseEntity<ApiResponse<CandidateSkillResponse>> {
        val userId = currentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
            candidateService.addSkill(userId, request),
            "Skill added successfully"
        ))
    }

    // PATCH /api/v1/candidate/skills/{id}?proficiencyLevel=ADVANCED
    @PatchMapping("/skills/{id}")
    fun updateSkill(
        @PathVariable id: String,
        @RequestParam proficiencyLevel: ProficiencyLevel
    ): ResponseEntity<ApiResponse<CandidateSkillResponse>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(
            candidateService.updateSkill(userId, id, proficiencyLevel),
            "Skill updated successfully"
        ))
    }

    // DELETE /api/v1/candidate/skills/{id}
    @DeleteMapping("/skills/{id}")
    fun removeSkill(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = currentUserId()
        candidateService.removeSkill(userId, id)
        return ResponseEntity.ok(ApiResponse.ok(null, "Skill removed successfully"))
    }

    // GET /api/v1/candidate/experiences
    @GetMapping("/experiences")
    fun getExperiences(): ResponseEntity<ApiResponse<List<ExperienceResponse>>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(candidateService.getExperiences(userId)))
    }

    // POST /api/v1/candidate/experiences
    @PostMapping("/experiences")
    fun addExperience(
        @Valid @RequestBody request: ExperienceRequest
    ): ResponseEntity<ApiResponse<ExperienceResponse>> {
        val userId = currentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
            candidateService.addExperience(userId, request),
            "Experience added successfully"
        ))
    }

    // PUT /api/v1/candidate/experiences/{id}
    @PutMapping("/experiences/{id}")
    fun updateExperience(
        @PathVariable id: String,
        @Valid @RequestBody request: ExperienceRequest
    ): ResponseEntity<ApiResponse<ExperienceResponse>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(
            candidateService.updateExperience(userId, id, request),
            "Experience updated successfully"
        ))
    }

    // DELETE /api/v1/candidate/experiences/{id}
    @DeleteMapping("/experiences/{id}")
    fun deleteExperience(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = currentUserId()
        candidateService.deleteExperience(userId, id)
        return ResponseEntity.ok(ApiResponse.ok(null, "Experience deleted successfully"))
    }

    // GET /api/v1/candidate/educations
    @GetMapping("/educations")
    fun getEducations(): ResponseEntity<ApiResponse<List<EducationResponse>>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(candidateService.getEducations(userId)))
    }

    // POST /api/v1/candidate/educations
    @PostMapping("/educations")
    fun addEducation(
        @Valid @RequestBody request: EducationRequest
    ): ResponseEntity<ApiResponse<EducationResponse>> {
        val userId = currentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
            candidateService.addEducation(userId, request),
            "Education added successfully"
        ))
    }

    // PUT /api/v1/candidate/educations/{id}
    @PutMapping("/educations/{id}")
    fun updateEducation(
        @PathVariable id: String,
        @Valid @RequestBody request: EducationRequest
    ): ResponseEntity<ApiResponse<EducationResponse>> {
        val userId = currentUserId()
        return ResponseEntity.ok(ApiResponse.ok(
            candidateService.updateEducation(userId, id, request),
            "Education updated successfully"
        ))
    }

    // DELETE /api/v1/candidate/educations/{id}
    @DeleteMapping("/educations/{id}")
    fun deleteEducation(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        val userId = currentUserId()
        candidateService.deleteEducation(userId, id)
        return ResponseEntity.ok(ApiResponse.ok(null, "Education deleted successfully"))
    }

    private fun currentUserId(): String =
        SecurityContextHolder.getContext().authentication.principal as String
}