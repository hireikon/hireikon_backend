package com.hireikon.hireikon_backend.database.model

import com.hireikon.hireikon_backend.database.model.enums.ProficiencyLevel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "candidate_skills",
    uniqueConstraints = [UniqueConstraint(columnNames = ["candidate_id", "skill_id"])]
)
class CandidateSkillEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    var candidate: CandidateProfileEntity = CandidateProfileEntity(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    var skill: SkillEntity = SkillEntity(),

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false)
    var proficiencyLevel: ProficiencyLevel = ProficiencyLevel.BEGINNER
)