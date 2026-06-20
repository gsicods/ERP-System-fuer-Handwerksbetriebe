package org.example.kalkulationsprogramm.repository.miete

import org.example.kalkulationsprogramm.domain.miete.Mietobjekt
import org.example.kalkulationsprogramm.domain.miete.Raum
import org.springframework.data.jpa.repository.JpaRepository

interface RaumRepository : JpaRepository<Raum, Long> {
    fun findByMietobjektOrderByNameAsc(mietobjekt: Mietobjekt): List<Raum>
}
