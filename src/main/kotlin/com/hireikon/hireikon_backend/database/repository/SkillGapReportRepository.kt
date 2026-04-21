package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.SkillGapReportEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SkillGapReportRepository: JpaRepository<SkillGapReportEntity, String> {
    fun findByApplicationId(applicationId: String): Optional<SkillGapReportEntity>
}