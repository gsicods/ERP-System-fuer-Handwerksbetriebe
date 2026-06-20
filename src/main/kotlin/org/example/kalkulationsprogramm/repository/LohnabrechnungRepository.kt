package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import java.util.Optional
import org.example.kalkulationsprogramm.domain.Lohnabrechnung
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LohnabrechnungRepository : JpaRepository<Lohnabrechnung, Long> {
    fun findByMitarbeiterIdOrderByJahrDescMonatDesc(mitarbeiterId: Long?): List<Lohnabrechnung>

    fun findByJahrOrderByMonatDescMitarbeiterNachnameAsc(jahr: Int?): List<Lohnabrechnung>

    fun findBySteuerberaterIdAndJahrOrderByMonatDescMitarbeiterNachnameAsc(
        steuerberaterId: Long?,
        jahr: Int?,
    ): List<Lohnabrechnung>

    fun findByMitarbeiterIdAndJahrAndMonat(
        mitarbeiterId: Long?,
        jahr: Int?,
        monat: Int?,
    ): Optional<Lohnabrechnung>

    @Query("SELECT l.jahr, COUNT(l) FROM Lohnabrechnung l GROUP BY l.jahr ORDER BY l.jahr DESC")
    fun countByJahrGrouped(): List<Array<Any>>

    @Query("SELECT DISTINCT l.jahr FROM Lohnabrechnung l ORDER BY l.jahr DESC")
    fun findDistinctJahre(): List<Int>

    fun findBySourceEmailId(emailId: Long?): List<Lohnabrechnung>

    fun existsBySourceEmailIdAndOriginalDateiname(emailId: Long?, originalDateiname: String): Boolean

    @Query(
        "SELECT COALESCE(SUM(l.bruttolohn), 0) FROM Lohnabrechnung l " +
            "WHERE l.mitarbeiter.id = :mitarbeiterId AND l.jahr = :jahr",
    )
    fun sumBruttolohnByMitarbeiterIdAndJahr(
        @Param("mitarbeiterId") mitarbeiterId: Long?,
        @Param("jahr") jahr: Int?,
    ): BigDecimal

    fun countByMitarbeiterIdAndJahr(mitarbeiterId: Long?, jahr: Int?): Long
}
