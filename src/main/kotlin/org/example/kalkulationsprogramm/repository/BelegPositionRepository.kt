package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.BelegPosition
import org.springframework.data.jpa.repository.JpaRepository

interface BelegPositionRepository : JpaRepository<BelegPosition, Long> {
    fun findByBelegIdOrderBySortierungAsc(belegId: Long?): List<BelegPosition>

    fun deleteByBelegId(belegId: Long?)
}
