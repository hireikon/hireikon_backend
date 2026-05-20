package com.hireikon.hireikon_backend.controller

import com.hireikon.hireikon_backend.dto.GenerateQuizRequest
import com.hireikon.hireikon_backend.dto.QuizHistoryResponse
import com.hireikon.hireikon_backend.dto.QuizResponse
import com.hireikon.hireikon_backend.dto.QuizResultResponse
import com.hireikon.hireikon_backend.dto.SubmitQuizRequest
import com.hireikon.hireikon_backend.service.QuizService
import com.hireikon.hireikon_backend.shared.ApiResponse
import com.hireikon.hireikon_backend.shared.CursorPage
import com.hireikon.hireikon_backend.shared.CursorRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/quiz")
class QuizController(
    private val quizService: QuizService
) {

    // POST /api/v1/quiz/generate
    @PostMapping("/generate")
    fun generateQuiz(
        @Valid @RequestBody request: GenerateQuizRequest
    ): ResponseEntity<ApiResponse<QuizResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.created(
                quizService.generateQuiz(currentUserId(), request),
                "Quiz generated successfully"
            )
        )

    // POST /api/v1/quiz/{id}/submit
    @PostMapping("/{id}/submit")
    fun submitQuiz(
        @PathVariable id: String,
        @Valid @RequestBody request: SubmitQuizRequest
    ): ResponseEntity<ApiResponse<QuizResultResponse>> =
        ResponseEntity.ok(
            ApiResponse.ok(
                quizService.submitQuiz(currentUserId(), id, request),
                "Quiz submitted successfully"
            )
        )

    // GET /api/v1/quiz/{id}
    @GetMapping("/{id}")
    fun getQuiz(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<QuizResponse>> =
        ResponseEntity.ok(ApiResponse.ok(quizService.getQuiz(currentUserId(), id)))

    // GET /api/v1/quiz/{id}/result
    @GetMapping("/{id}/result")
    fun getQuizResult(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<QuizResultResponse>> =
        ResponseEntity.ok(ApiResponse.ok(quizService.getQuizResult(currentUserId(), id)))

    // GET /api/v1/quiz/history?cursor=uuid&pageSize=20
    @GetMapping("/history")
    fun getQuizHistory(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<ApiResponse<CursorPage<QuizHistoryResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(
            quizService.getQuizHistory(currentUserId(), CursorRequest(cursor, pageSize))
        ))

    // GET /api/v1/quiz/history/skill?skillName=Kotlin&cursor=uuid&pageSize=20
    @GetMapping("/history/skill")
    fun getQuizHistoryBySkill(
        @RequestParam skillName: String,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<ApiResponse<CursorPage<QuizHistoryResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(
            quizService.getQuizHistoryBySkill(currentUserId(), skillName, CursorRequest(cursor, pageSize))
        ))

    private fun currentUserId(): String =
        SecurityContextHolder.getContext().authentication.principal as String
}