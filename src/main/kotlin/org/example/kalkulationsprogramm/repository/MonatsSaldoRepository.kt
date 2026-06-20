package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.MonatsSaldo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MonatsSaldoRepository : JpaRepository<MonatsSaldo, Long> {
    fun findByMitarbeiterIdAndJahrAndMonat(mitarbeiterId: Long?, jahr: Int?, monat: Int?): Optional<MonatsSaldo>

    fun findByMitarbeiterIdAndJahrAndGueltigTrue(mitarbeiterId: Long?, jahr: Int?): List<MonatsSaldo>

    fun findByMitarbeiterIdAndGueltigTrue(mitarbeiterId: Long?): List<MonatsSaldo>

    @Query(
        "SELECT ms FROM MonatsSaldo ms WHERE ms.mitarbeiter.id = :mitarbeiterId " +
            "AND ms.gueltig = true " +
            "AND (ms.jahr * 100 + ms.monat) >= :vonJahrMonat " +
            "AND (ms.jahr * 100 + ms.monat) <= :bisJahrMonat " +
            "ORDER BY ms.jahr, ms.monat",
    )
    fun findGueltigeImZeitraum(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("vonJahrMonat") vonJahrMonat: Int,
        @Param("bisJahrMonat") bisJahrMonat: Int,
    ): List<MonatsSaldo>

    @Modifying
    @Query("UPDATE MonatsSaldo ms SET ms.gueltig = false WHERE ms.mitarbeiter.id = :mitarbeiterId AND ms.jahr = :jahr AND ms.monat = :monat")
    fun invalidiere(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("jahr") jahr: Int,
        @Param("monat") monat: Int,
    )

    @Modifying
    @Query("UPDATE MonatsSaldo ms SET ms.gueltig = false WHERE ms.mitarbeiter.id = :mitarbeiterId AND ms.jahr = :jahr")
    fun invalidiereJahr(@Param("mitarbeiterId") mitarbeiterId: Long?, @Param("jahr") jahr: Int)

    @Modifying
    @Query("UPDATE MonatsSaldo ms SET ms.gueltig = false WHERE ms.mitarbeiter.id = :mitarbeiterId")
    fun invalidiereAlle(@Param("mitarbeiterId") mitarbeiterId: Long?)
}
