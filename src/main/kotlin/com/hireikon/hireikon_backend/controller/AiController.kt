package com.hireikon.hireikon_backend.controller

import com.hireikon.hireikon_backend.ai.QuizGenerator
import com.hireikon.hireikon_backend.ai.ResumeParser
import com.hireikon.hireikon_backend.ai.SkillGapAnalyzer
import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.dto.GeneratedQuizDto
import com.hireikon.hireikon_backend.dto.ParsedResumeDto
import com.hireikon.hireikon_backend.dto.SkillGapReportDto
import com.hireikon.hireikon_backend.service.StorageService
import com.hireikon.hireikon_backend.shared.ApiResponse
import com.hireikon.hireikon_backend.shared.BadRequestException
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/ai")
class AiController(
    private val resumeParser: ResumeParser,
    private val skillGapAnalyzer: SkillGapAnalyzer,
    private val quizGenerator: QuizGenerator,
    private val storageService: StorageService
) {

    // POST /api/v1/ai/resume/parse
    @PostMapping("/resume/parse", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun parseResume(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ApiResponse<ParsedResumeDto>> {
        if (file.contentType != MediaType.APPLICATION_PDF_VALUE) {
            throw BadRequestException("Only PDF files are accepted")
        }
        val parsed = resumeParser.parse(file.bytes)
        return ResponseEntity.ok(
            ApiResponse.ok(
                parsed,
                "Resume parsed successfully"
            )
        )
    }

    // POST /api/v1/ai/resume/parse-and-fill
    @PostMapping("/resume/parse-and-fill", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun parseAndFillProfile(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ApiResponse<ParsedResumeDto>> {
        if (file.contentType != MediaType.APPLICATION_PDF_VALUE) {
            throw BadRequestException("Only PDF files are accepted")
        }
        val userId = currentUserId()

        val resumeUrl = storageService.uploadResume(file, userId)
        val parsed = resumeParser.parseAndPopulateProfile(file.bytes, userId, resumeUrl)

        return ResponseEntity.ok(
            ApiResponse.ok(
                parsed,
                "Resume uploaded and profile populated successfully"
            )
        )
    }

    // GET /api/v1/ai/skill-gap?jobId={jobId}
    // Full AI-powered skill gap analysis for a specific job
    @GetMapping("/skill-gap")
    fun analyzeSkillGap(
        @RequestParam jobId: String
    ): ResponseEntity<ApiResponse<SkillGapReportDto>> {
        val userId = currentUserId()
        val report = skillGapAnalyzer.analyze(userId, jobId)
        return ResponseEntity.ok(
            ApiResponse.ok(
                report,
                "Skill gap analysis complete"
            )
        )
    }

    // GET /api/v1/ai/match-score?jobId={jobId}
    @GetMapping("/match-score")
    fun getMatchScore(
        @RequestParam jobId: String
    ): ResponseEntity<ApiResponse<Map<String, Int>>> {
        val userId = currentUserId()
        val score = skillGapAnalyzer.calculateLocalMatchScore(userId, jobId)
        return ResponseEntity.ok(
            ApiResponse.ok(
                mapOf("matchScore" to score),
                "Match score calculated"
            )
        )
    }

    // GET /api/v1/ai/quiz?skillName=Kotlin&proficiencyLevel=INTERMEDIATE&questionCount=5
    @GetMapping("/quiz")
    fun generateQuiz(
        @RequestParam skillName: String,
        @RequestParam proficiencyLevel: ProficiencyLevel,
        @RequestParam(defaultValue = "5") questionCount: Int
    ): ResponseEntity<ApiResponse<GeneratedQuizDto>> {
        if (questionCount !in 3..10) {
            throw BadRequestException("questionCount must be between 3 and 10")
        }
        val quiz = quizGenerator.generate(skillName, proficiencyLevel, questionCount)
        return ResponseEntity.ok(ApiResponse.ok(quiz, "Quiz generated successfully"))
    }

    private fun currentUserId(): String =
        SecurityContextHolder.getContext().authentication.principal as String
}