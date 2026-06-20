package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.ZeitbuchungAudit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ZeitbuchungAuditRepository : JpaRepository<ZeitbuchungAudit, Long> {
    fun findByZeitbuchungIdOrderByVersionDesc(zeitbuchungId: Long?): List<ZeitbuchungAudit>

    fun findByZeitbuchungIdOrderByVersionAsc(zeitbuchungId: Long?): List<ZeitbuchungAudit>

    fun existsByZeitbuchungId(zeitbuchungId: Long?): Boolean

    fun countByZeitbuchungId(zeitbuchungId: Long?): Long
}
