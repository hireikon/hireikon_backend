package com.hireikon.hireikon_backend.database.model

import com.hireikon.hireikon_backend.database.model.enums.SkillCategory
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "skills")
class SkillEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @Column(nullable = false, unique = true)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: SkillCategory = SkillCategory.OTHER,

    @OneToMany(mappedBy = "skill", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var candidateSkills: MutableList<CandidateSkillEntity> = mutableListOf(),

    @OneToMany(mappedBy = "skill", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var jobRequiredSkills: MutableList<JobRequiredSkillEntity> = mutableListOf(),

    @OneToMany(mappedBy = "skill", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var quizzes: MutableList<QuizEntity> = mutableListOf()
)