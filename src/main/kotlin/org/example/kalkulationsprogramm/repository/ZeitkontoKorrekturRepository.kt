package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import java.time.LocalDate
import org.example.kalkulationsprogramm.domain.ZeitkontoKorrektur
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ZeitkontoKorrekturRepository : JpaRepository<ZeitkontoKorrektur, Long> {
    fun findByMitarbeiterIdOrderByDatumDesc(mitarbeiterId: Long?): List<ZeitkontoKorrektur>

    @Query("SELECT k FROM ZeitkontoKorrektur k WHERE k.mitarbeiter.id = :mitarbeiterId AND k.datum >= :von AND k.datum <= :bis ORDER BY k.datum ASC")
    fun findByMitarbeiterIdAndDatumBetween(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate,
    ): List<ZeitkontoKorrektur>

    @Query("SELECT COALESCE(SUM(k.stunden), 0) FROM ZeitkontoKorrektur k WHERE k.mitarbeiter.id = :mitarbeiterId AND k.datum >= :von AND k.datum <= :bis")
    fun sumStundenByMitarbeiterIdAndDatumBetween(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate,
    ): BigDecimal

    @Query("SELECT COALESCE(SUM(k.stunden), 0) FROM ZeitkontoKorrektur k WHERE k.mitarbeiter.id = :mitarbeiterId AND YEAR(k.datum) = :jahr")
    fun sumStundenByMitarbeiterIdAndJahr(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("jahr") jahr: Int,
    ): BigDecimal
}
