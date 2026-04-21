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

@Entity
@Table(name = "recruiters")
class RecruiterEntity(

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String = "",

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: UserEntity = UserEntity(),

    @Column(name = "company_name", nullable = false)
    var companyName: String = "",

    @Column(nullable = false)
    var position: String = "",

    @OneToMany(mappedBy = "recruiter", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var jobs: MutableList<JobEntity> = mutableListOf()
)