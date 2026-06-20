package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.SystemSetting
import org.springframework.data.jpa.repository.JpaRepository

interface SystemSettingRepository : JpaRepository<SystemSetting, String> {
    fun findByKeyStartingWith(prefix: String): List<SystemSetting>
}
