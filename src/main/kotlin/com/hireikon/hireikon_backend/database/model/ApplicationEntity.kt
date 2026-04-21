package com.hireikon.hireikon_backend.database.model

import com.hireikon.hireikon_backend.database.model.enums.ApplicationStatus
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "applications",
    uniqueConstraints = [UniqueConstraint(columnNames = ["candidate_id", "job_id"])]
)
class ApplicationEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    var candidate: CandidateProfileEntity = CandidateProfileEntity(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    var job: JobEntity = JobEntity(),

    // 0–100, computed by AI engine and cached here
    @Column(name = "match_score")
    var matchScore: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ApplicationStatus = ApplicationStatus.PENDING,

    @Column(name = "applied_at", nullable = false, updatable = false)
    var appliedAt: LocalDateTime = LocalDateTime.now(),

    @OneToOne(mappedBy = "application", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var skillGapReport: SkillGapReportEntity? = null
)