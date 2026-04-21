package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.ExperienceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExperienceRepository: JpaRepository<ExperienceEntity, String> {
    fun findByCandidateIdOrderByStartDateDesc(candidateId: String): List<ExperienceEntity>
}