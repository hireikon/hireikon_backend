package com.hireikon.hireikon_backend.ai

import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.database.model.enums.SkillCategory
import com.hireikon.hireikon_backend.dto.AddSkillRequest
import com.hireikon.hireikon_backend.dto.EducationRequest
import com.hireikon.hireikon_backend.dto.ExperienceRequest
import com.hireikon.hireikon_backend.dto.ParsedResumeDto
import com.hireikon.hireikon_backend.dto.UpdateProfileRequest
import com.hireikon.hireikon_backend.service.CandidateService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class ResumeParser(
    private val geminiClient: GeminiClient,
    private val candidateService: CandidateService,
    private val resumeProfileSaver: ResumeProfileSaver
) {
    private val systemInstruction = """
        You are an expert resume parser. Extract structured information from the provided PDF resume.
        Always respond with valid JSON only — no markdown, no explanation, no extra text.
        If a field is not found in the resume, use null.
        For dates, always use "YYYY-MM-DD" format. If only year is given, use "YYYY-01-01".
        For proficiencyLevel, infer from context: use one of BEGINNER, INTERMEDIATE, ADVANCED, EXPERT.
        For skill category, use one of: PROGRAMMING, FRAMEWORK, DATABASE, CLOUD, DEVOPS, DESIGN, SOFT_SKILL, LANGUAGE, OTHER.
    """.trimIndent()

    private val userPrompt = """
        Extract all information from this resume and return ONLY a JSON object with this exact structure:
        {
          "fullName": "string",
          "email": "string or null",
          "phone": "string or null",
          "location": "string or null",
          "summary": "string or null",
          "linkedinUrl": "full LinkedIn profile URL or null",
          "githubUrl": "full GitHub profile URL or null",
          "skills": [
            {
              "name": "string",
              "category": "PROGRAMMING|FRAMEWORK|DATABASE|CLOUD|DEVOPS|DESIGN|SOFT_SKILL|LANGUAGE|OTHER",
              "proficiencyLevel": "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT"
            }
          ],
          "experiences": [
            {
              "company": "string",
              "title": "string",
              "startDate": "YYYY-MM-DD",
              "endDate": "YYYY-MM-DD or null",
              "description": "string or null"
            }
          ],
          "educations": [
            {
              "institution": "string",
              "degree": "string",
              "field": "string",
              "graduationDate": "YYYY-MM-DD or null"
            }
          ]
        }
    """.trimIndent()

    fun parse(pdfBytes: ByteArray): ParsedResumeDto {
        val response = geminiClient.promptWithPdf(systemInstruction, userPrompt, pdfBytes)
        return geminiClient.parseJson(response, ParsedResumeDto::class.java)
    }

    fun parseAndPopulateProfile(
        pdfBytes: ByteArray,
        userId: String,
        resumeUrl: String? = null
    ): ParsedResumeDto {
        val parsed = parse(pdfBytes)

        resumeProfileSaver.saveProfile(userId, parsed, resumeUrl)

        parsed.skills.forEach { skillDto ->
            resumeProfileSaver.saveSkillSafely(userId, skillDto)
        }

        parsed.experiences.forEach { expDto ->
            resumeProfileSaver.saveExperienceSafely(userId, expDto)
        }

        parsed.educations.forEach { eduDto ->
            resumeProfileSaver.saveEducationSafely(userId, eduDto)
        }

        return parsed
    }
}