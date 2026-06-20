package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Abteilung
import org.springframework.data.jpa.repository.JpaRepository

interface AbteilungRepository : JpaRepository<Abteilung, Long> {
    fun findByName(name: String): Optional<Abteilung>
}
