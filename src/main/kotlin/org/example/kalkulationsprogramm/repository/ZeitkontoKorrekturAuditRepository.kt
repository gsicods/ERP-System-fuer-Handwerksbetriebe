package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.ZeitkontoKorrekturAudit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ZeitkontoKorrekturAuditRepository : JpaRepository<ZeitkontoKorrekturAudit, Long> {
    fun findByZeitkontoKorrekturIdOrderByVersionDesc(zeitkontoKorrekturId: Long?): List<ZeitkontoKorrekturAudit>
}
