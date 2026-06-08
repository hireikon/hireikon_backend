package com.hireikon.hireikon_backend.database.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "candidate_profiles")
class CandidateProfileEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: UserEntity = UserEntity(),

    @Column(name = "full_name", nullable = false)
    var fullName: String = "",

    @Column
    var phone: String? = null,

    @Column
    var location: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "resume_url")
    var resumeUrl: String? = null,

    @Column(name = "linkedin_url")
    var linkedinUrl: String? = null,

    @Column(name = "github_url")
    var githubUrl: String? = null,

    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "candidate", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var skills: MutableList<CandidateSkillEntity> = mutableListOf(),

    @OneToMany(mappedBy = "candidate", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var experiences: MutableList<ExperienceEntity> = mutableListOf(),

    @OneToMany(mappedBy = "candidate", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var educations: MutableList<EducationEntity> = mutableListOf(),

    @OneToMany(mappedBy = "candidate", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var applications: MutableList<ApplicationEntity> = mutableListOf(),

    @OneToMany(mappedBy = "candidate", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var quizzes: MutableList<QuizEntity> = mutableListOf()
)