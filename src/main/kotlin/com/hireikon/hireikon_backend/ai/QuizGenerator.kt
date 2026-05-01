package com.hireikon.hireikon_backend.ai

import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.database.repository.SkillRepository
import com.hireikon.hireikon_backend.dto.GeneratedQuizDto
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import org.springframework.stereotype.Component

@Component
class QuizGenerator(
    private val geminiClient: GeminiClient,
    private val skillRepository: SkillRepository
) {
    private val systemInstruction = """
        You are a technical interviewer creating skill assessment quizzes.
        Always respond with valid JSON only — no markdown, no explanation, no extra text.
        Questions must be practical and test real-world knowledge, not just theory.
        Each question must have exactly 4 options labeled A, B, C, D.
        The correctAnswer must be exactly one of: "A", "B", "C", "D".
    """.trimIndent()

    fun generate(
        skillName: String,
        proficiencyLevel: ProficiencyLevel,
        questionCount: Int = 5
    ): GeneratedQuizDto {
        val skill = skillRepository.findByNameIgnoreCase(skillName)
            .orElseThrow { ResourceNotFoundException("Skill '$skillName' not found") }

        val difficulty = when (proficiencyLevel) {
            ProficiencyLevel.BEGINNER -> "beginner-friendly, fundamental concepts"
            ProficiencyLevel.INTERMEDIATE -> "intermediate level, practical application"
            ProficiencyLevel.ADVANCED -> "advanced level, edge cases and best practices"
            ProficiencyLevel.EXPERT -> "expert level, deep internals and architecture decisions"
        }

        val prompt = """
            Generate $questionCount multiple choice questions to assess $difficulty knowledge of "${skill.name}".
 
            Return ONLY a JSON object with this exact structure:
            {
              "skillName": "${skill.name}",
              "questions": [
                {
                  "question": "string",
                  "options": ["A. string", "B. string", "C. string", "D. string"],
                  "correctAnswer": "A|B|C|D",
                  "explanation": "string explaining why the answer is correct"
                }
              ]
            }
        """.trimIndent()

        val response = geminiClient.prompt(systemInstruction, prompt)
        return geminiClient.parseJson(response, GeneratedQuizDto::class.java)
    }
}