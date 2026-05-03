package com.hireikon.hireikon_backend.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hireikon.hireikon_backend.ai.QuizGenerator
import com.hireikon.hireikon_backend.database.model.CandidateProfileEntity
import com.hireikon.hireikon_backend.database.model.QuizEntity
import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import com.hireikon.hireikon_backend.database.repository.CandidateProfileRepository
import com.hireikon.hireikon_backend.database.repository.QuizRepository
import com.hireikon.hireikon_backend.database.repository.SkillRepository
import com.hireikon.hireikon_backend.dto.GenerateQuizRequest
import com.hireikon.hireikon_backend.dto.QuizHistoryResponse
import com.hireikon.hireikon_backend.dto.QuizQuestionDto
import com.hireikon.hireikon_backend.dto.QuizQuestionResponse
import com.hireikon.hireikon_backend.dto.QuizResponse
import com.hireikon.hireikon_backend.dto.QuizResultQuestionResponse
import com.hireikon.hireikon_backend.dto.QuizResultResponse
import com.hireikon.hireikon_backend.dto.SubmitQuizRequest
import com.hireikon.hireikon_backend.shared.BadRequestException
import com.hireikon.hireikon_backend.shared.ResourceNotFoundException
import com.hireikon.hireikon_backend.shared.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QuizService(
    private val quizRepository: QuizRepository,
    private val candidateProfileRepository: CandidateProfileRepository,
    private val skillRepository: SkillRepository,
    private val quizGenerator: QuizGenerator,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun generateQuiz(userId: String, request: GenerateQuizRequest): QuizResponse {
        if (request.questionCount !in 10..30) {
            throw BadRequestException("questionCount must be between 10 and 30")
        }

        val profile = findProfile(userId)
        val skill = skillRepository.findByNameIgnoreCase(request.skillName)
            .orElseThrow { ResourceNotFoundException("Skill '${request.skillName}' not found. Add it to your profile first.") }

        // Generate questions via Gemini
        val generated = quizGenerator.generate(skill.name, request.proficiencyLevel, request.questionCount)

        // Store full questions JSON (including correct answers) in DB
        // Correct answers are NOT sent to the client until after submission
        val questionsJson = objectMapper.writeValueAsString(generated.questions)

        val quiz = quizRepository.save(
            QuizEntity(
                candidate = profile,
                skill = skill,
                proficiencyLevel = request.proficiencyLevel,
                questions = questionsJson,
                score = null    // null until submitted
            )
        )

        return quiz.toResponse(generated.questions)
    }

    // Submit answers and get scored result
    @Transactional
    fun submitQuiz(userId: String, quizId: String, request: SubmitQuizRequest): QuizResultResponse {
        val profile = findProfile(userId)
        val quiz = findOwnedQuiz(quizId, profile.id)

        if (quiz.score != null) {
            throw BadRequestException("This quiz has already been submitted")
        }

        val questions = parseQuestions(quiz.questions)

        if (request.answers.size != questions.size) {
            throw BadRequestException(
                "Expected ${questions.size} answers but got ${request.answers.size}"
            )
        }

        // Validate all answers are valid option letters
        val validOptions = setOf("A", "B", "C", "D")
        request.answers.forEach { answer ->
            if (answer.uppercase() !in validOptions) {
                throw BadRequestException("Invalid answer '$answer'. Must be A, B, C, or D")
            }
        }

        // Score the quiz
        val results = questions.mapIndexed { index, question ->
            val candidateAnswer = request.answers[index].uppercase()
            val isCorrect = candidateAnswer == question.correctAnswer.uppercase()
            QuizResultQuestionResponse(
                index = index + 1,
                question = question.question,
                options = question.options,
                correctAnswer = question.correctAnswer,
                candidateAnswer = candidateAnswer,
                isCorrect = isCorrect,
                explanation = question.explanation
            )
        }

        val correctCount = results.count { it.isCorrect }
        val score = (correctCount.toDouble() / questions.size * 100).toInt()

        // Persist score
        quiz.score = score
        quiz.candidateAnswers = objectMapper.writeValueAsString(
            request.answers.map { it.uppercase() }
        )
        quizRepository.save(quiz)

        return QuizResultResponse(
            id = quiz.id,
            skillName = quiz.skill.name,
            proficiencyLevel = quiz.proficiencyLevel,
            score = score,
            correctCount = correctCount,
            totalQuestions = questions.size,
            questions = results
        )
    }

    // Get a single quiz (without answers)
    @Transactional(readOnly = true)
    fun getQuiz(userId: String, quizId: String): QuizResponse {
        val profile = findProfile(userId)
        val quiz = findOwnedQuiz(quizId, profile.id)
        val questions = parseQuestions(quiz.questions)
        return quiz.toResponse(questions)
    }

    // Get quiz result (with correct answers — only after submission)
    @Transactional(readOnly = true)
    fun getQuizResult(userId: String, quizId: String): QuizResultResponse {
        val profile = findProfile(userId)
        val quiz = findOwnedQuiz(quizId, profile.id)

        if (quiz.score == null) {
            throw BadRequestException("This quiz has not been submitted yet")
        }

        val questions = parseQuestions(quiz.questions)
        val candidateAnswers = parseCandidateAnswers(quiz.candidateAnswers)

        // Reconstruct results from stored questions
        // Note: candidate answers are not stored separately — this endpoint
        // only shows questions + correct answers, not which option the candidate picked
        val resultQuestions = questions.mapIndexed { index, q ->
            val candidateAnswer = candidateAnswers.getOrElse(index) { "" }
            val isCorrect = candidateAnswer == q.correctAnswer.uppercase()
            QuizResultQuestionResponse(
                index = index + 1,
                question = q.question,
                options = q.options,
                correctAnswer = q.correctAnswer,
                candidateAnswer = candidateAnswer,
                isCorrect = isCorrect,
                explanation = q.explanation
            )
        }

        val correctCount = resultQuestions.count { it.isCorrect }

        return QuizResultResponse(
            id = quiz.id,
            skillName = quiz.skill.name,
            proficiencyLevel = quiz.proficiencyLevel,
            score = quiz.score!!,
            correctCount = correctCount,
            totalQuestions = questions.size,
            questions = resultQuestions
        )
    }

    // Quiz history for a candidate
    @Transactional(readOnly = true)
    fun getQuizHistory(userId: String): List<QuizHistoryResponse> {
        val profile = findProfile(userId)
        return quizRepository.findByCandidateId(profile.id)
            .map { it.toHistoryResponse() }
    }

    @Transactional(readOnly = true)
    fun getQuizHistoryBySkill(userId: String, skillName: String): List<QuizHistoryResponse> {
        val profile = findProfile(userId)
        val skill = skillRepository.findByNameIgnoreCase(skillName)
            .orElseThrow { ResourceNotFoundException("Skill '$skillName' not found") }
        return quizRepository.findByCandidateIdAndSkillId(profile.id, skill.id)
            .map { it.toHistoryResponse() }
    }

    private fun findProfile(userId: String): CandidateProfileEntity =
        candidateProfileRepository.findByUserId(userId)
            .orElseThrow { ResourceNotFoundException("Candidate profile not found") }

    private fun findOwnedQuiz(quizId: String, profileId: String): QuizEntity {
        val quiz = quizRepository.findById(quizId)
            .orElseThrow { ResourceNotFoundException("Quiz not found") }
        if (quiz.candidate.id != profileId) throw UnauthorizedException()
        return quiz
    }

    private fun parseQuestions(json: String): List<QuizQuestionDto> =
        objectMapper.readValue(json, object : TypeReference<List<QuizQuestionDto>>() {})

    private fun parseCandidateAnswers(json: String?): List<String> {
        if (json == null) return emptyList()
        return objectMapper.readValue(json, object : TypeReference<List<String>>() {})
    }

    private fun QuizEntity.toResponse(questions: List<QuizQuestionDto>) = QuizResponse(
        id = id,
        skillName = skill.name,
        proficiencyLevel = proficiencyLevel,
        // Strip correct answers and explanations — only send question + options
        questions = questions.mapIndexed { index, q ->
            QuizQuestionResponse(
                index = index + 1,
                question = q.question,
                options = q.options
            )
        },
        score = score,
        submitted = score != null,
        takenAt = takenAt.toString()
    )

    private fun QuizEntity.toHistoryResponse() = QuizHistoryResponse(
        id = id,
        skillName = skill.name,
        proficiencyLevel = ProficiencyLevel.INTERMEDIATE,
        score = score,
        submitted = score != null,
        takenAt = takenAt.toString()
    )
}