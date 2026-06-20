package org.example.kalkulationsprogramm.repository.miete

import org.example.kalkulationsprogramm.domain.miete.Kostenstelle
import org.springframework.data.jpa.repository.JpaRepository

interface MieteKostenstelleRepository : JpaRepository<Kostenstelle, Long> {
    fun findByMietobjektIdOrderByNameAsc(mietobjektId: Long?): List<Kostenstelle>
}
