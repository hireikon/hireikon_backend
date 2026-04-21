package com.hireikon.hireikon_backend.database.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "quizzes")
class QuizEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    var candidate: CandidateProfileEntity = CandidateProfileEntity(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    var skill: SkillEntity = SkillEntity(),

    // JSON array of AI-generated questions with candidate answers + correct answers
    // e.g. [{"question": "...", "options": [...], "correct": "B", "answer": "B"}]
    @Column(columnDefinition = "jsonb", nullable = false)
    var questions: String = "[]",

    // 0–100 percentage score
    @Column
    var score: Int? = null,

    @Column(name = "taken_at", nullable = false, updatable = false)
    var takenAt: LocalDateTime = LocalDateTime.now()
)