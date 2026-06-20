package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Krankenkasse
import org.springframework.data.jpa.repository.JpaRepository

interface KrankenkasseRepository : JpaRepository<Krankenkasse, Long> {
    fun findAllByOrderByNameAsc(): List<Krankenkasse>

    fun findByAktivTrueOrderByNameAsc(): List<Krankenkasse>
}
