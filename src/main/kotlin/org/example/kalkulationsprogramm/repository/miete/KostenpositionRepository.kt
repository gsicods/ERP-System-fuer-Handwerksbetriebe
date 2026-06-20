package org.example.kalkulationsprogramm.repository.miete

import org.example.kalkulationsprogramm.domain.miete.Kostenposition
import org.example.kalkulationsprogramm.domain.miete.Kostenstelle
import org.springframework.data.jpa.repository.JpaRepository

interface KostenpositionRepository : JpaRepository<Kostenposition, Long> {
    fun findByKostenstelle(kostenstelle: Kostenstelle): List<Kostenposition>

    fun findByKostenstelleAndAbrechnungsJahr(kostenstelle: Kostenstelle, abrechnungsJahr: Int?): List<Kostenposition>

    fun findByKostenstelleMietobjektIdAndAbrechnungsJahr(
        mietobjektId: Long?,
        abrechnungsJahr: Int?,
    ): List<Kostenposition>
}
