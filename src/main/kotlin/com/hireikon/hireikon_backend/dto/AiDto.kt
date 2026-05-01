package com.hireikon.hireikon_backend.dto

// What Gemini extracts from a PDF resume
data class ParsedResumeDto(
    val fullName: String,
    val email: String?,
    val phone: String?,
    val location: String?,
    val summary: String?,
    val linkedinUrl: String?,
    val githubUrl: String?,
    val skills: List<ParsedSkillDto>,
    val experiences: List<ParsedExperienceDto>,
    val educations: List<ParsedEducationDto>
)

data class ParsedSkillDto(
    val name: String,
    val category: String,           // maps to SkillCategory enum
    val proficiencyLevel: String    // maps to ProficiencyLevel enum
)

data class ParsedExperienceDto(
    val company: String?,
    val title: String?,
    val startDate: String?,          // "YYYY-MM-DD" — parsed to LocalDate in service
    val endDate: String?,           // null = currently working
    val description: String?
)

data class ParsedEducationDto(
    val institution: String?,
    val degree: String?,
    val field: String?,
    val graduationDate: String?     // "YYYY-MM-DD" or null
)

// Full skill gap report returned to the candidate
data class SkillGapReportDto(
    val matchScore: Int,                        // 0–100
    val matchedSkills: List<MatchedSkillDto>,
    val missingSkills: List<MissingSkillDto>,
    val learningRoadmap: Map<String, RoadmapItemDto>
)

data class MatchedSkillDto(
    val skillName: String,
    val candidateLevel: String,
    val requiredLevel: String,
    val meetsRequirement: Boolean
)

data class MissingSkillDto(
    val skillName: String,
    val requiredLevel: String,
    val importance: String,         // "CRITICAL", "IMPORTANT", "NICE_TO_HAVE"
    val reason: String
)

data class RoadmapItemDto(
    val skillName: String,
    val estimatedWeeks: Int,
    val resources: List<LearningResourceDto>
)

data class LearningResourceDto(
    val title: String,
    val type: String,               // "COURSE", "DOCUMENTATION", "BOOK", "YOUTUBE", "PRACTICE"
    val url: String?,
    val isFree: Boolean
)

data class GeneratedQuizDto(
    val skillName: String,
    val questions: List<QuizQuestionDto>
)

data class QuizQuestionDto(
    val question: String,
    val options: List<String>,      // always 4 options (A, B, C, D)
    val correctAnswer: String,      // "A", "B", "C", or "D"
    val explanation: String
)