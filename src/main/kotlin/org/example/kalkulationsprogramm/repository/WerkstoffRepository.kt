package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Werkstoff
import org.springframework.data.jpa.repository.JpaRepository

interface WerkstoffRepository : JpaRepository<Werkstoff, Long> {
    fun findByNameIgnoreCase(name: String): Optional<Werkstoff>
}
