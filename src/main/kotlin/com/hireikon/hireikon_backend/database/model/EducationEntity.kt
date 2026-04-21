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
import java.time.LocalDate

@Entity
@Table(name = "educations")
class EducationEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    var candidate: CandidateProfileEntity = CandidateProfileEntity(),

    @Column(nullable = false)
    var institution: String = "",

    @Column(nullable = false)
    var degree: String = "",

    @Column(nullable = false)
    var field: String = "",

    @Column(name = "graduation_date")
    var graduationDate: LocalDate? = null
)