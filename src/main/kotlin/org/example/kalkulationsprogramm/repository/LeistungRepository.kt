package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Leistung
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeistungRepository : JpaRepository<Leistung, Long> {
    fun findByBezeichnungContainingIgnoreCase(search: String): List<Leistung>
}
