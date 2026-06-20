package org.example.kalkulationsprogramm.repository.miete

import org.example.kalkulationsprogramm.domain.miete.Raum
import org.example.kalkulationsprogramm.domain.miete.Verbrauchsart
import org.example.kalkulationsprogramm.domain.miete.Verbrauchsgegenstand
import org.springframework.data.jpa.repository.JpaRepository

interface VerbrauchsgegenstandRepository : JpaRepository<Verbrauchsgegenstand, Long> {
    fun findByRaumOrderByNameAsc(raum: Raum): List<Verbrauchsgegenstand>

    fun findByRaumMietobjektId(mietobjektId: Long?): List<Verbrauchsgegenstand>

    fun findByRaumMietobjektIdAndVerbrauchsart(
        mietobjektId: Long?,
        verbrauchsart: Verbrauchsart,
    ): List<Verbrauchsgegenstand>
}
