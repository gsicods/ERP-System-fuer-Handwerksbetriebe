package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import org.example.kalkulationsprogramm.domain.ProjektArt
import org.example.kalkulationsprogramm.domain.Zeitbuchung
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ZeitbuchungRepository : JpaRepository<Zeitbuchung, Long> {
    fun findByMitarbeiterIdAndEndeZeitIsNull(mitarbeiterId: Long?): List<Zeitbuchung>

    fun findFirstByMitarbeiterIdAndEndeZeitIsNullOrderByStartZeitDesc(mitarbeiterId: Long?): Optional<Zeitbuchung>

    @Query("SELECT z FROM Zeitbuchung z WHERE z.mitarbeiter.id = :mitarbeiterId AND z.startZeit >= :startTime ORDER BY z.startZeit ASC")
    fun findByMitarbeiterIdAndStartZeitAfter(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("startTime") startTime: LocalDateTime,
    ): List<Zeitbuchung>

    @Query("SELECT z FROM Zeitbuchung z WHERE z.mitarbeiter.id = :mitarbeiterId AND z.startZeit >= :startTime AND z.startZeit <= :endTime ORDER BY z.startZeit ASC")
    fun findByMitarbeiterIdAndStartZeitBetween(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
    ): List<Zeitbuchung>

    fun findFirstByMitarbeiterIdOrderByStartZeitAsc(mitarbeiterId: Long?): Optional<Zeitbuchung>

    fun findByProjektId(projektId: Long?): List<Zeitbuchung>

    @Query("SELECT z FROM Zeitbuchung z WHERE z.projekt.id = :projektId AND z.startZeit >= :von AND z.startZeit < :bis ORDER BY z.startZeit ASC")
    fun findByProjektIdAndZeitraum(
        @Param("projektId") projektId: Long?,
        @Param("von") von: LocalDateTime,
        @Param("bis") bis: LocalDateTime,
    ): List<Zeitbuchung>

    fun countByArbeitsgangId(arbeitsgangId: Long?): Long

    fun deleteByProjektId(projektId: Long?)

    fun findByProjektIdAndArbeitsgangIdAndProjektProduktkategorieId(
        projektId: Long?,
        arbeitsgangId: Long?,
        projektProduktkategorieId: Long?,
    ): Optional<Zeitbuchung>

    fun findByIdempotencyKey(idempotencyKey: String): Optional<Zeitbuchung>

    fun findByStopIdempotencyKey(stopIdempotencyKey: String): Optional<Zeitbuchung>

    fun existsByProjektProduktkategorieId(projektProduktkategorieId: Long?): Boolean

    @Query(
        "SELECT COALESCE(SUM(z.anzahlInStunden), 0) FROM Zeitbuchung z " +
            "WHERE z.mitarbeiter.id = :mitarbeiterId " +
            "AND z.startZeit >= :von AND z.startZeit < :bis " +
            "AND z.projekt.projektArt IN :arten " +
            "AND z.typ = org.example.kalkulationsprogramm.domain.BuchungsTyp.ARBEIT",
    )
    fun sumStundenByMitarbeiterAndProjektArtAndZeitraum(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("von") von: LocalDateTime,
        @Param("bis") bis: LocalDateTime,
        @Param("arten") arten: List<ProjektArt>,
    ): BigDecimal
}
