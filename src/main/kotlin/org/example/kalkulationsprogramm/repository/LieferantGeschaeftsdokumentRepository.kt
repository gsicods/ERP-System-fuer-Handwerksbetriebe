package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.LieferantGeschaeftsdokument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LieferantGeschaeftsdokumentRepository : JpaRepository<LieferantGeschaeftsdokument, Long> {
    fun findByDokumentNummer(dokumentNummer: String): Optional<LieferantGeschaeftsdokument>

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "WHERE gd.dokument.lieferant.id = :lieferantId " +
            "AND gd.dokumentNummer = :referenzNummer",
    )
    fun findByLieferantIdAndDokumentNummer(
        @Param("lieferantId") lieferantId: Long?,
        @Param("referenzNummer") referenzNummer: String,
    ): List<LieferantGeschaeftsdokument>

    @Query(
        "SELECT CASE WHEN COUNT(gd) > 0 THEN true ELSE false END " +
            "FROM LieferantGeschaeftsdokument gd " +
            "WHERE gd.dokument.lieferant.id = :lieferantId " +
            "AND gd.dokumentNummer = :dokumentNummer " +
            "AND gd.betragBrutto = :betragBrutto",
    )
    fun existsByLieferantIdAndDokumentNummerAndBetragBrutto(
        @Param("lieferantId") lieferantId: Long?,
        @Param("dokumentNummer") dokumentNummer: String,
        @Param("betragBrutto") betragBrutto: BigDecimal,
    ): Boolean

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "JOIN gd.dokument d " +
            "WHERE d.typ IN (org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.RECHNUNG, org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.GUTSCHRIFT) " +
            "AND gd.bezahlt = false " +
            "AND (gd.bereitsGezahlt IS NULL OR gd.bereitsGezahlt = false) " +
            "ORDER BY gd.zahlungsziel ASC NULLS LAST, gd.dokumentDatum ASC",
    )
    fun findAllOffeneEingangsrechnungen(): List<LieferantGeschaeftsdokument>

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "JOIN gd.dokument d " +
            "WHERE d.typ IN (org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.RECHNUNG, org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.GUTSCHRIFT) " +
            "AND gd.bezahlt = false " +
            "AND (gd.bereitsGezahlt IS NULL OR gd.bereitsGezahlt = false) " +
            "AND gd.genehmigt = true " +
            "ORDER BY gd.zahlungsziel ASC NULLS LAST, gd.dokumentDatum ASC",
    )
    fun findAllOffeneGenehmigte(): List<LieferantGeschaeftsdokument>

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "JOIN gd.dokument d " +
            "WHERE d.typ IN (org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.RECHNUNG, org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.GUTSCHRIFT) " +
            "ORDER BY gd.dokumentDatum DESC",
    )
    fun findAllEingangsrechnungen(): List<LieferantGeschaeftsdokument>

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "JOIN gd.dokument d " +
            "WHERE d.typ IN (org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.RECHNUNG, org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.GUTSCHRIFT) " +
            "AND gd.genehmigt = true " +
            "ORDER BY gd.dokumentDatum DESC",
    )
    fun findAllGenehmigte(): List<LieferantGeschaeftsdokument>

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "JOIN gd.dokument d " +
            "WHERE d.typ IN (org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.RECHNUNG, org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.GUTSCHRIFT) " +
            "AND gd.dokumentDatum BETWEEN :startDate AND :endDate",
    )
    fun findRechnungenByDatumBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<LieferantGeschaeftsdokument>

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "WHERE gd.dokumentDatum BETWEEN :startDate AND :endDate " +
            "ORDER BY gd.dokumentDatum DESC",
    )
    fun findAllByDatumBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<LieferantGeschaeftsdokument>

    @Query("SELECT gd FROM LieferantGeschaeftsdokument gd ORDER BY gd.dokumentDatum DESC")
    fun findAllSortedByDatum(): List<LieferantGeschaeftsdokument>

    @Query(
        "SELECT gd FROM LieferantGeschaeftsdokument gd " +
            "JOIN gd.dokument d " +
            "WHERE d.typ = org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.RECHNUNG " +
            "AND gd.bezahlt = true " +
            "AND gd.bezahltAm BETWEEN :startDate AND :endDate",
    )
    fun findBezahlteRechnungenByBezahltAmBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<LieferantGeschaeftsdokument>

    @Query(
        value = "SELECT AVG(DATEDIFF(gd.liefertermin, gd.dokument_datum)) " +
            "FROM lieferant_geschaeftsdokument gd " +
            "JOIN lieferant_dokument d ON gd.id = d.id " +
            "WHERE d.lieferant_id = :lieferantId " +
            "AND d.typ = 'AUFTRAGSBESTAETIGUNG' " +
            "AND gd.dokument_datum IS NOT NULL " +
            "AND gd.liefertermin IS NOT NULL",
        nativeQuery = true,
    )
    fun calculateAverageLieferzeitByLieferantId(@Param("lieferantId") lieferantId: Long?): Double?

    @Query(
        value = "SELECT COUNT(*) FROM lieferant_dokument d " +
            "WHERE d.lieferant_id = :lieferantId " +
            "AND d.typ = 'AUFTRAGSBESTAETIGUNG'",
        nativeQuery = true,
    )
    fun countBestellungenByLieferantId(@Param("lieferantId") lieferantId: Long?): Long?

    @Query(
        value = "SELECT COALESCE(SUM(gd.betrag_netto), 0) " +
            "FROM lieferant_geschaeftsdokument gd " +
            "JOIN lieferant_dokument d ON gd.id = d.id " +
            "WHERE d.lieferant_id = :lieferantId " +
            "AND d.typ = 'RECHNUNG'",
        nativeQuery = true,
    )
    fun sumGesamtkostenByLieferantId(@Param("lieferantId") lieferantId: Long?): Double?

    @Query(
        value = "SELECT YEAR(gd.dokument_datum) as jahr, " +
            "COUNT(DISTINCT d.id) as bestellungen, " +
            "COALESCE(SUM(gd.betrag_netto), 0) as netto " +
            "FROM lieferant_geschaeftsdokument gd " +
            "JOIN lieferant_dokument d ON gd.id = d.id " +
            "WHERE d.typ = 'RECHNUNG' " +
            "AND gd.dokument_datum IS NOT NULL " +
            "GROUP BY YEAR(gd.dokument_datum) " +
            "ORDER BY YEAR(gd.dokument_datum) DESC",
        nativeQuery = true,
    )
    fun getLieferantenkostenProJahr(): List<Array<Any>>

    @Query(
        value = "SELECT l.lieferantenname, " +
            "COUNT(CASE WHEN d.typ = 'AUFTRAGSBESTAETIGUNG' THEN 1 END) as bestellungen, " +
            "SUM(CASE WHEN d.typ = 'RECHNUNG' THEN gd.betrag_netto ELSE 0 END) as netto " +
            "FROM lieferanten l " +
            "LEFT JOIN lieferant_dokument d ON d.lieferant_id = l.id " +
            "LEFT JOIN lieferant_geschaeftsdokument gd ON gd.id = d.id " +
            "GROUP BY l.id, l.lieferantenname " +
            "HAVING COUNT(d.id) > 0 " +
            "ORDER BY netto DESC",
        nativeQuery = true,
    )
    fun getLieferantenPerformance(): List<Array<Any>>

    @Query(
        value = "SELECT l.lieferantenname, " +
            "COUNT(CASE WHEN d.typ = 'AUFTRAGSBESTAETIGUNG' THEN 1 END) as bestellungen, " +
            "SUM(CASE WHEN d.typ = 'RECHNUNG' THEN gd.betrag_netto ELSE 0 END) as netto " +
            "FROM lieferanten l " +
            "LEFT JOIN lieferant_dokument d ON d.lieferant_id = l.id " +
            "LEFT JOIN lieferant_geschaeftsdokument gd ON gd.id = d.id " +
            "WHERE (gd.dokument_datum IS NULL OR " +
            "       (YEAR(gd.dokument_datum) = :jahr AND (:monat IS NULL OR MONTH(gd.dokument_datum) = :monat))) " +
            "GROUP BY l.id, l.lieferantenname " +
            "HAVING SUM(CASE WHEN d.typ = 'RECHNUNG' THEN gd.betrag_netto ELSE 0 END) > 0 " +
            "ORDER BY netto DESC",
        nativeQuery = true,
    )
    fun getLieferantenPerformanceFiltered(
        @Param("jahr") jahr: Int?,
        @Param("monat") monat: Int?,
    ): List<Array<Any>>

    @Query(
        "SELECT CASE WHEN COUNT(gd) > 0 THEN true ELSE false END " +
            "FROM LieferantGeschaeftsdokument gd " +
            "WHERE gd.dokument.lieferant.id = :lieferantId " +
            "AND gd.dokumentNummer = :dokumentNummer",
    )
    fun existsByLieferantIdAndDokumentNummer(
        @Param("lieferantId") lieferantId: Long?,
        @Param("dokumentNummer") dokumentNummer: String,
    ): Boolean

    @Query(
        value = "SELECT gd.id, gd.dokument_nummer, d.lieferant_id, l.lieferantenname " +
            "FROM lieferant_geschaeftsdokument gd " +
            "JOIN lieferant_dokument d ON gd.id = d.id " +
            "JOIN lieferanten l ON d.lieferant_id = l.id " +
            "WHERE gd.dokument_nummer IS NOT NULL " +
            "AND (d.lieferant_id, gd.dokument_nummer) IN (" +
            "   SELECT d2.lieferant_id, gd2.dokument_nummer " +
            "   FROM lieferant_geschaeftsdokument gd2 " +
            "   JOIN lieferant_dokument d2 ON gd2.id = d2.id " +
            "   WHERE gd2.dokument_nummer IS NOT NULL " +
            "   GROUP BY d2.lieferant_id, gd2.dokument_nummer " +
            "   HAVING COUNT(*) > 1" +
            ") " +
            "ORDER BY d.lieferant_id, gd.dokument_nummer, gd.id",
        nativeQuery = true,
    )
    fun findAllDuplicates(): List<Array<Any>>
}
