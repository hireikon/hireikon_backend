package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.EducationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EducationRepository: JpaRepository<EducationEntity, String> {
    fun findByCandidateIdOrderByGraduationDateDesc(candidateId: String): List<EducationEntity>
}