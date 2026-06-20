package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import java.time.LocalDate
import org.example.kalkulationsprogramm.domain.Abwesenheit
import org.example.kalkulationsprogramm.domain.AbwesenheitsTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AbwesenheitRepository : JpaRepository<Abwesenheit, Long> {
    fun findByMitarbeiterIdOrderByDatumDesc(mitarbeiterId: Long?): List<Abwesenheit>

    @Query("SELECT a FROM Abwesenheit a WHERE a.mitarbeiter.id = :mitarbeiterId AND a.datum >= :von AND a.datum <= :bis ORDER BY a.datum ASC")
    fun findByMitarbeiterIdAndDatumBetween(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate,
    ): List<Abwesenheit>

    fun findByMitarbeiterIdAndTypOrderByDatumDesc(mitarbeiterId: Long?, typ: AbwesenheitsTyp): List<Abwesenheit>

    fun findByUrlaubsantragId(urlaubsantragId: Long?): List<Abwesenheit>

    fun deleteByUrlaubsantragId(urlaubsantragId: Long?)

    @Query("SELECT COUNT(a) FROM Abwesenheit a WHERE a.mitarbeiter.id = :mitarbeiterId AND a.typ = :typ AND YEAR(a.datum) = :jahr")
    fun countByMitarbeiterIdAndTypAndJahr(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("typ") typ: AbwesenheitsTyp,
        @Param("jahr") jahr: Int,
    ): Long

    @Query("SELECT COALESCE(SUM(a.stunden), 0) FROM Abwesenheit a WHERE a.mitarbeiter.id = :mitarbeiterId AND a.datum >= :von AND a.datum <= :bis")
    fun sumStundenByMitarbeiterIdAndDatumBetween(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate,
    ): BigDecimal

    @Query("SELECT COALESCE(SUM(a.stunden), 0) FROM Abwesenheit a WHERE a.mitarbeiter.id = :mitarbeiterId AND a.typ = :typ AND a.datum >= :von AND a.datum <= :bis")
    fun sumStundenByMitarbeiterIdAndTypAndDatumBetween(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("typ") typ: AbwesenheitsTyp,
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate,
    ): BigDecimal

    fun existsByMitarbeiterIdAndDatumAndTyp(
        mitarbeiterId: Long?,
        datum: LocalDate,
        typ: AbwesenheitsTyp,
    ): Boolean

    @Query("SELECT a FROM Abwesenheit a JOIN FETCH a.mitarbeiter WHERE a.datum >= :von AND a.datum <= :bis ORDER BY a.datum ASC, a.mitarbeiter.nachname ASC")
    fun findAllByDatumBetween(@Param("von") von: LocalDate, @Param("bis") bis: LocalDate): List<Abwesenheit>
}
