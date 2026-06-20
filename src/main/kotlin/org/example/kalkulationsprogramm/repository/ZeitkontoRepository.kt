package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Mitarbeiter
import org.example.kalkulationsprogramm.domain.Zeitkonto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ZeitkontoRepository : JpaRepository<Zeitkonto, Long> {
    fun findByMitarbeiter(mitarbeiter: Mitarbeiter): Optional<Zeitkonto>

    fun findByMitarbeiterId(mitarbeiterId: Long?): Optional<Zeitkonto>

    fun existsByMitarbeiterId(mitarbeiterId: Long?): Boolean
}
