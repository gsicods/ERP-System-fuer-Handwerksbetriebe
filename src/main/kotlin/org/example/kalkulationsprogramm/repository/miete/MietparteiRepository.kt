package org.example.kalkulationsprogramm.repository.miete

import org.example.kalkulationsprogramm.domain.miete.Mietobjekt
import org.example.kalkulationsprogramm.domain.miete.Mietpartei
import org.example.kalkulationsprogramm.domain.miete.MietparteiRolle
import org.springframework.data.jpa.repository.JpaRepository

interface MietparteiRepository : JpaRepository<Mietpartei, Long> {
    fun findByMietobjektOrderByNameAsc(mietobjekt: Mietobjekt): List<Mietpartei>

    fun findByMietobjektAndRolle(mietobjekt: Mietobjekt, rolle: MietparteiRolle): List<Mietpartei>
}
