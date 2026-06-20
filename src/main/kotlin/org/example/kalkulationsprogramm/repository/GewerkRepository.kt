package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Gewerk
import org.springframework.data.jpa.repository.JpaRepository

interface GewerkRepository : JpaRepository<Gewerk, Long> {
    fun findAllByOrderByNameAsc(): List<Gewerk>

    fun findByAktivTrueOrderByNameAsc(): List<Gewerk>
}
