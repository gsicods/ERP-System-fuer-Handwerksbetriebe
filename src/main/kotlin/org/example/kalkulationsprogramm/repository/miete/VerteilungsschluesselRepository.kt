package org.example.kalkulationsprogramm.repository.miete

import org.example.kalkulationsprogramm.domain.miete.Mietobjekt
import org.example.kalkulationsprogramm.domain.miete.Verteilungsschluessel
import org.springframework.data.jpa.repository.JpaRepository

interface VerteilungsschluesselRepository : JpaRepository<Verteilungsschluessel, Long> {
    fun findByMietobjektOrderByNameAsc(mietobjekt: Mietobjekt): List<Verteilungsschluessel>
}
