package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Arbeitsgang
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArbeitsgangRepository : JpaRepository<Arbeitsgang, Long> {
    fun findByBeschreibung(bezeichnung: String): Optional<Arbeitsgang>
}
