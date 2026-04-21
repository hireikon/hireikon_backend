package com.hireikon.hireikon_backend.database.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "skill_gap_reports")
class SkillGapReportEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    var application: ApplicationEntity = ApplicationEntity(),

    // JSON array of missing skill names + gap details from Gemini
    // e.g. [{"skill": "Kubernetes", "importance": "HIGH", "reason": "..."}]
    @Column(name = "missing_skills", columnDefinition = "jsonb", nullable = false)
    var missingSkills: String = "[]",

    // JSON object with learning path per skill from Gemini
    // e.g. {"Kubernetes": {"resources": [...], "estimatedWeeks": 4}}
    @Column(name = "learning_roadmap", columnDefinition = "jsonb", nullable = false)
    var learningRoadmap: String = "{}",

    @Column(name = "generated_at", nullable = false, updatable = false)
    var generatedAt: LocalDateTime = LocalDateTime.now()
)