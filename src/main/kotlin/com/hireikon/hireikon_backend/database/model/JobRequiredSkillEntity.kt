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
    name = "job_required_skills",
    uniqueConstraints = [UniqueConstraint(columnNames = ["job_id", "skill_id"])]
)
class JobRequiredSkillEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    var job: JobEntity = JobEntity(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    var skill: SkillEntity = SkillEntity(),

    @Enumerated(EnumType.STRING)
    @Column(name = "level_required", nullable = false)
    var levelRequired: ProficiencyLevel = ProficiencyLevel.BEGINNER,

    @Column(name = "is_mandatory", nullable = false)
    var isMandatory: Boolean = true
)