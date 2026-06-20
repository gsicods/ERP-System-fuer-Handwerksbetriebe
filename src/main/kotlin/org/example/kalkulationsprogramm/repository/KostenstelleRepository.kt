package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Kostenstelle
import org.example.kalkulationsprogramm.domain.KostenstellenTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KostenstelleRepository : JpaRepository<Kostenstelle, Long> {
    fun findByAktivTrueOrderBySortierungAsc(): List<Kostenstelle>

    fun findByTypAndAktivTrue(typ: KostenstellenTyp): List<Kostenstelle>

    fun findByBezeichnung(bezeichnung: String): Optional<Kostenstelle>

    fun findGemeinkosten(): List<Kostenstelle> = findByTypAndAktivTrue(KostenstellenTyp.GEMEINKOSTEN)

    fun findLager(): List<Kostenstelle> = findByTypAndAktivTrue(KostenstellenTyp.LAGER)
}
