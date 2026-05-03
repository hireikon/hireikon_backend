package com.hireikon.hireikon_backend.dto

import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

// Generate Quiz
data class GenerateQuizRequest(
    @field:NotBlank(message = "Skill name is required")
    val skillName: String,
    val proficiencyLevel: ProficiencyLevel = ProficiencyLevel.INTERMEDIATE,
    val questionCount: Int = 10
)

// Quiz Response (before submission — no correct answers exposed)
data class QuizResponse(
    val id: String,
    val skillName: String,
    val proficiencyLevel: ProficiencyLevel,
    val questions: List<QuizQuestionResponse>,
    val score: Int?,    // null until submitted
    val submitted: Boolean,
    val takenAt: String
)

data class QuizQuestionResponse(
    val index: Int,
    val question: String,
    val options: List<String>    // correct answer NOT included until after submission
)

// Submit Quiz
data class SubmitQuizRequest(
    @field:NotEmpty(message = "Answers cannot be empty")
    val answers: List<String>    // ["A", "B", "C", "D", "A"] — one per question
)

// Quiz Result (after submission — correct answers revealed)
data class QuizResultResponse(
    val id: String,
    val skillName: String,
    val proficiencyLevel: ProficiencyLevel,
    val score: Int,    // 0–100
    val correctCount: Int,
    val totalQuestions: Int,
    val questions: List<QuizResultQuestionResponse>
)

data class QuizResultQuestionResponse(
    val index: Int,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,    // revealed after submission
    val candidateAnswer: String,
    val isCorrect: Boolean,
    val explanation: String
)

// Quiz History
data class QuizHistoryResponse(
    val id: String,
    val skillName: String,
    val proficiencyLevel: ProficiencyLevel,
    val score: Int?,
    val submitted: Boolean,
    val takenAt: String
)