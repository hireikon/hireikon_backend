package com.hireikon.hireikon_backend.database.repository

import com.hireikon.hireikon_backend.database.model.SkillEntity
import com.hireikon.hireikon_backend.database.model.enums.SkillCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SkillRepository: JpaRepository<SkillEntity, String> {
    fun findByNameIgnoreCase(name: String): Optional<SkillEntity>
    fun findByCategory(category: SkillCategory): List<SkillEntity>
    fun existsByNameIgnoreCase(name: String): Boolean
}