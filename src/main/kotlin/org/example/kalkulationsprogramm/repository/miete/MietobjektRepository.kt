package org.example.kalkulationsprogramm.repository.miete

import java.util.Optional
import org.example.kalkulationsprogramm.domain.miete.Mietobjekt
import org.springframework.data.jpa.repository.JpaRepository

interface MietobjektRepository : JpaRepository<Mietobjekt, Long> {
    fun findByNameIgnoreCase(name: String): Optional<Mietobjekt>
}
