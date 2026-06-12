package com.hireikon.hireikon_backend.database.model

import com.hireikon.hireikon_backend.database.model.enums.JobStatus
import com.hireikon.hireikon_backend.database.model.enums.JobType
import com.hireikon.hireikon_backend.database.model.enums.SalaryPeriod
import com.hireikon.hireikon_backend.database.model.enums.WorkMode
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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "jobs")
class JobEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    var recruiter: RecruiterEntity = RecruiterEntity(),

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var company: String = "",

    @Column
    var location: String? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    var description: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.DRAFT,

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    var jobType: JobType = JobType.FULL_TIME,

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false)
    var workMode: WorkMode = WorkMode.ON_SITE,

    @Column(name = "salary_min")
    var salaryMin: Long? = null,

    @Column(name = "salary_Max")
    var salaryMax: Long? = null,

    @Column(name = "salary_currency", length = 10)
    var salaryCurrency: String? = null,  // e.g. "BDT", "USD"

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_period")
    var salaryPeriod: SalaryPeriod? = null,

    @Column(name = "posted_at")
    var postedAt: LocalDateTime? = null,

    @Column(name = "deadline")
    var deadline: LocalDateTime? = null,

    @OneToMany(mappedBy = "job", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var requiredSkills: MutableList<JobRequiredSkillEntity> = mutableListOf(),

    @OneToMany(mappedBy = "job", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var applications: MutableList<ApplicationEntity> = mutableListOf()
)