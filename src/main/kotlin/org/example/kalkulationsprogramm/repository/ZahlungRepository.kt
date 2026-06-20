package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Zahlung
import org.example.kalkulationsprogramm.domain.ZahlungRichtung
import org.example.kalkulationsprogramm.domain.ZahlungStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDate

interface ZahlungRepository : JpaRepository<Zahlung, Long> {
    fun findByAusgangsDokumentIdAndStatusOrderByZahlungsdatumDesc(
        dokumentId: Long,
        status: ZahlungStatus
    ): List<Zahlung>

    fun findByBelegIdAndStatusOrderByZahlungsdatumDesc(
        belegId: Long,
        status: ZahlungStatus
    ): List<Zahlung>

    @Query(
        "SELECT COALESCE(SUM(z.betrag), 0) FROM Zahlung z " +
            "WHERE z.status = org.example.kalkulationsprogramm.domain.ZahlungStatus.ERFASST " +
            "AND z.richtung = :richtung " +
            "AND z.zahlungsdatum BETWEEN :von AND :bis"
    )
    fun summeByRichtungImZeitraum(
        @Param("richtung") richtung: ZahlungRichtung,
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate
    ): BigDecimal?

    @Query(
        "SELECT COALESCE(SUM(z.betrag), 0) FROM Zahlung z " +
            "WHERE z.status = org.example.kalkulationsprogramm.domain.ZahlungStatus.ERFASST " +
            "AND z.ausgangsDokument.id = :dokumentId"
    )
    fun summeByAusgangsDokument(@Param("dokumentId") dokumentId: Long?): BigDecimal?

    @Query(
        "SELECT COALESCE(SUM(z.betrag), 0) FROM Zahlung z " +
            "WHERE z.status = org.example.kalkulationsprogramm.domain.ZahlungStatus.ERFASST " +
            "AND z.beleg.id = :belegId"
    )
    fun summeByBeleg(@Param("belegId") belegId: Long?): BigDecimal?
}
