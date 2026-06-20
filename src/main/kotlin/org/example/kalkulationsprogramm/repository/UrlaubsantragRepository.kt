package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import org.example.kalkulationsprogramm.domain.Urlaubsantrag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UrlaubsantragRepository : JpaRepository<Urlaubsantrag, Long> {
    fun findByMitarbeiterId(mitarbeiterId: Long?): List<Urlaubsantrag>

    fun findByStatus(status: Urlaubsantrag.Status): List<Urlaubsantrag>

    fun findByMitarbeiterIdOrderByVonDatumDesc(mitarbeiterId: Long?): List<Urlaubsantrag>

    @Query(
        "SELECT u FROM Urlaubsantrag u WHERE u.mitarbeiter.id = :mitarbeiterId " +
            "AND u.status != 'ABGELEHNT' " +
            "AND ((u.vonDatum <= :end) AND (u.bisDatum >= :start))",
    )
    fun findOverlapping(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("start") start: LocalDate,
        @Param("end") end: LocalDate,
    ): List<Urlaubsantrag>

    fun findByMitarbeiterIdAndVonDatumBetweenOrderByVonDatumDesc(
        mitarbeiterId: Long?,
        start: LocalDate,
        end: LocalDate,
    ): List<Urlaubsantrag>

    fun findByMitarbeiterIdAndStatusOrderByVonDatumDesc(
        mitarbeiterId: Long?,
        status: Urlaubsantrag.Status,
    ): List<Urlaubsantrag>
}
