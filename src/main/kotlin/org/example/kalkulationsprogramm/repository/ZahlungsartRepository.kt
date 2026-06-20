package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Zahlungsart
import org.springframework.data.jpa.repository.JpaRepository

interface ZahlungsartRepository : JpaRepository<Zahlungsart, Long> {
    fun findAllByOrderBySortierungAscBezeichnungAsc(): List<Zahlungsart>

    fun findByAktivTrueOrderBySortierungAscBezeichnungAsc(): List<Zahlungsart>
}
