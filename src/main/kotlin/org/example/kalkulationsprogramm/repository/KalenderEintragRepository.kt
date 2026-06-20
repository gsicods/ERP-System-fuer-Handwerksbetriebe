package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import org.example.kalkulationsprogramm.domain.KalenderEintrag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface KalenderEintragRepository : JpaRepository<KalenderEintrag, Long> {
    @Query(
        "SELECT k FROM KalenderEintrag k " +
            "LEFT JOIN FETCH k.projekt " +
            "LEFT JOIN FETCH k.kunde " +
            "LEFT JOIN FETCH k.lieferant " +
            "LEFT JOIN FETCH k.anfrage " +
            "LEFT JOIN FETCH k.ersteller " +
            "WHERE k.datum BETWEEN :von AND :bis " +
            "ORDER BY k.datum, k.startZeit",
    )
    fun findByDatumBetween(@Param("von") von: LocalDate, @Param("bis") bis: LocalDate): List<KalenderEintrag>

    @Query(
        "SELECT DISTINCT k FROM KalenderEintrag k " +
            "LEFT JOIN FETCH k.projekt " +
            "LEFT JOIN FETCH k.kunde " +
            "LEFT JOIN FETCH k.lieferant " +
            "LEFT JOIN FETCH k.anfrage " +
            "LEFT JOIN FETCH k.ersteller " +
            "LEFT JOIN k.teilnehmer t " +
            "WHERE k.datum BETWEEN :von AND :bis " +
            "AND (k.ersteller.id = :mitarbeiterId " +
            "     OR t.id = :mitarbeiterId " +
            "     OR (k.ersteller IS NULL AND SIZE(k.teilnehmer) = 0)) " +
            "ORDER BY k.datum, k.startZeit",
    )
    fun findByMitarbeiterAndDatumBetween(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate,
    ): List<KalenderEintrag>

    @Query(
        "SELECT DISTINCT k FROM KalenderEintrag k " +
            "LEFT JOIN FETCH k.projekt " +
            "LEFT JOIN FETCH k.kunde " +
            "LEFT JOIN FETCH k.lieferant " +
            "LEFT JOIN FETCH k.anfrage " +
            "LEFT JOIN FETCH k.ersteller " +
            "LEFT JOIN k.teilnehmer t " +
            "WHERE k.datum = :datum " +
            "AND (k.ersteller.id = :mitarbeiterId " +
            "     OR t.id = :mitarbeiterId " +
            "     OR (k.ersteller IS NULL AND SIZE(k.teilnehmer) = 0)) " +
            "ORDER BY k.startZeit",
    )
    fun findByMitarbeiterAndDatum(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("datum") datum: LocalDate,
    ): List<KalenderEintrag>

    fun findByProjektIdOrderByDatumDesc(projektId: Long?): List<KalenderEintrag>

    fun findByKundeIdOrderByDatumDesc(kundeId: Long?): List<KalenderEintrag>

    fun findByLieferantIdOrderByDatumDesc(lieferantId: Long?): List<KalenderEintrag>

    fun findByAnfrageIdOrderByDatumDesc(anfrageId: Long?): List<KalenderEintrag>

    @Query("SELECT k FROM KalenderEintrag k LEFT JOIN FETCH k.teilnehmer WHERE k.id = :id")
    fun findByIdWithTeilnehmer(@Param("id") id: Long?): KalenderEintrag
}
