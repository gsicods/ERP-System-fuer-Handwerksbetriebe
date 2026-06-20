package org.example.kalkulationsprogramm.repository.miete

import java.util.Optional
import org.example.kalkulationsprogramm.domain.miete.Verbrauchsgegenstand
import org.example.kalkulationsprogramm.domain.miete.Zaehlerstand
import org.springframework.data.jpa.repository.JpaRepository

interface ZaehlerstandRepository : JpaRepository<Zaehlerstand, Long> {
    fun findByVerbrauchsgegenstandAndAbrechnungsJahr(
        verbrauchsgegenstand: Verbrauchsgegenstand,
        abrechnungsJahr: Int?,
    ): Optional<Zaehlerstand>

    fun findByVerbrauchsgegenstandOrderByAbrechnungsJahrDesc(
        verbrauchsgegenstand: Verbrauchsgegenstand,
    ): List<Zaehlerstand>

    fun findByVerbrauchsgegenstandInAndAbrechnungsJahr(
        gegenstaende: Collection<Verbrauchsgegenstand>,
        abrechnungsJahr: Int?,
    ): List<Zaehlerstand>
}
