package com.hireikon.hireikon_backend.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.hireikon.hireikon_backend.database.repository.CandidateProfileRepository
import com.hireikon.hireikon_backend.database.repository.CandidateSkillRepository
import com.hireikon.hireikon_backend.database.repository.JobRequiredSkillRepository
import com.hireikon.hireikon_backend.dto.SkillGapReportDto
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import org.springframework.stereotype.Component

@Component
class SkillGapAnalyzer(
    private val geminiClient: GeminiClient,
    private val candidateProfileRepository: CandidateProfileRepository,
    private val candidateSkillRepository: CandidateSkillRepository,
    private val jobRequiredSkillRepository: JobRequiredSkillRepository,
    private val objectMapper: ObjectMapper
) {
    private val systemInstruction = """
        You are an expert technical recruiter and career advisor.
        Analyze the gap between a candidate's skills and a job's requirements.
        Always respond with valid JSON only — no markdown, no explanation, no extra text.
        Be specific and practical in your recommendations.
        For learning resources, prefer free resources when available but include paid ones if they are significantly better.
    """.trimIndent()

    fun analyze(userId: String, jobId: String): SkillGapReportDto {
        val profile = candidateProfileRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Candidate profile not found") }

        val candidateSkills = candidateSkillRepository.findByCandidateId(profile.id)
        val jobRequiredSkills = jobRequiredSkillRepository.findByJobId(jobId)

        if (jobRequiredSkills.isEmpty()) return emptyReport()

        // Build plain-text summaries to send to Gemini
        val candidateSkillsSummary = candidateSkills.joinToString("\n") {
            "- ${it.skill.name} (${it.proficiencyLevel})"
        }.ifEmpty { "No skills listed" }

        val jobSkillsSummary = jobRequiredSkills.joinToString("\n") {
            "- ${it.skill.name} (required: ${it.levelRequired}, mandatory: ${it.isMandatory})"
        }

        val prompt = buildAnalysisPrompt(candidateSkillsSummary, jobSkillsSummary)
        val response = geminiClient.prompt(systemInstruction, prompt)
        return geminiClient.parseJson(response, SkillGapReportDto::class.java)
    }

    fun calculateLocalMatchScore(userId: String, jobId: String): Int {
        val profile = candidateProfileRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Candidate profile not found") }

        val candidateSkillIds = candidateSkillRepository
            .findByCandidateId(profile.id)
            .map { it.skill.id }
            .toSet()

        val jobRequiredSkills = jobRequiredSkillRepository.findByJobId(jobId)

        if (jobRequiredSkills.isEmpty()) return 0

        val mandatorySkills = jobRequiredSkills.filter { it.isMandatory }
        val optionalSkills = jobRequiredSkills.filter { !it.isMandatory }

        // Mandatory skills = 70% of score, optional = 30%
        val mandatoryScore = if (mandatorySkills.isEmpty()) 70 else {
            val matched = mandatorySkills.count { it.skill.id in candidateSkillIds }
            (matched.toDouble() / mandatorySkills.size * 70).toInt()
        }

        val optionalScore = if (optionalSkills.isEmpty()) 30 else {
            val matched = optionalSkills.count { it.skill.id in candidateSkillIds }
            (matched.toDouble() / optionalSkills.size * 30).toInt()
        }

        return (mandatoryScore + optionalScore).coerceIn(0, 100)
    }

    fun serializeMissingSkills(report: SkillGapReportDto): String =
        objectMapper.writeValueAsString(report.missingSkills)

    fun serializeLearningRoadmap(report: SkillGapReportDto): String =
        objectMapper.writeValueAsString(report.learningRoadmap)

    private fun buildAnalysisPrompt(
        candidateSkills: String,
        jobSkills: String
    ): String = """
        Analyze the skill gap between this candidate and job requirements.
 
        CANDIDATE SKILLS:
        $candidateSkills
 
        JOB REQUIRED SKILLS:
        $jobSkills
 
        Return ONLY a JSON object with this exact structure:
        {
          "matchScore": <integer 0-100>,
          "matchedSkills": [
            {
              "skillName": "string",
              "candidateLevel": "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT",
              "requiredLevel": "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT",
              "meetsRequirement": true|false
            }
          ],
          "missingSkills": [
            {
              "skillName": "string",
              "requiredLevel": "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT",
              "importance": "CRITICAL|IMPORTANT|NICE_TO_HAVE",
              "reason": "string explaining why this skill matters for this job"
            }
          ],
          "learningRoadmap": {
            "SkillName": {
              "skillName": "string",
              "estimatedWeeks": <integer>,
              "resources": [
                {
                  "title": "string",
                  "type": "COURSE|DOCUMENTATION|BOOK|YOUTUBE|PRACTICE",
                  "url": "string or null",
                  "isFree": true|false
                }
              ]
            }
          }
        }
    """.trimIndent()

    private fun emptyReport() = SkillGapReportDto(
        matchScore = 0,
        matchedSkills = emptyList(),
        missingSkills = emptyList(),
        learningRoadmap = emptyMap()
    )
}