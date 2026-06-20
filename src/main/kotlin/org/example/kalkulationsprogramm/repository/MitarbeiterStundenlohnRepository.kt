package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.MitarbeiterStundenlohn
import org.springframework.data.jpa.repository.JpaRepository

interface MitarbeiterStundenlohnRepository : JpaRepository<MitarbeiterStundenlohn, Long> {
    fun findByMitarbeiterIdOrderByGueltigAbDesc(mitarbeiterId: Long?): List<MitarbeiterStundenlohn>

    fun findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(
        mitarbeiterId: Long?,
        stichtag: LocalDate,
    ): Optional<MitarbeiterStundenlohn>
}
