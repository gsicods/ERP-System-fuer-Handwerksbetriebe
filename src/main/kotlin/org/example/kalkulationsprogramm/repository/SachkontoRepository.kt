package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Sachkonto
import org.springframework.data.jpa.repository.JpaRepository

interface SachkontoRepository : JpaRepository<Sachkonto, Long> {
    fun findAllByOrderBySortierungAscBezeichnungAsc(): List<Sachkonto>

    fun findByAktivTrueOrderBySortierungAscBezeichnungAsc(): List<Sachkonto>

    fun findByNummer(nummer: String): Optional<Sachkonto>
}
