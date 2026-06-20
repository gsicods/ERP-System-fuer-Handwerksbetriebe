package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import java.time.LocalDate
import org.example.kalkulationsprogramm.domain.Beleg
import org.example.kalkulationsprogramm.domain.BelegKategorie
import org.example.kalkulationsprogramm.domain.BelegKiAnalyseStatus
import org.example.kalkulationsprogramm.domain.BelegStatus
import org.example.kalkulationsprogramm.domain.Mitarbeiter
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BelegRepository : JpaRepository<Beleg, Long> {
    fun findByStatusOrderByUploadDatumDesc(status: BelegStatus): List<Beleg>

    fun findByStatusAndBelegKategorieOrderByBelegDatumDesc(
        status: BelegStatus,
        kategorie: BelegKategorie,
    ): List<Beleg>

    fun findAllByOrderByUploadDatumDesc(): List<Beleg>

    @Query(
        "SELECT b FROM Beleg b " +
            "LEFT JOIN FETCH b.lieferant " +
            "LEFT JOIN FETCH b.kostenstelle " +
            "WHERE b.status <> org.example.kalkulationsprogramm.domain.BelegStatus.VERWORFEN " +
            "  AND b.kostenstelle IS NULL " +
            "  AND NOT EXISTS (SELECT a.id FROM BelegKostenstellenAnteil a WHERE a.beleg = b) " +
            "  AND NOT EXISTS (SELECT d.id FROM LieferantDokument d WHERE d.beleg = b) " +
            "ORDER BY b.belegDatum DESC, b.uploadDatum DESC",
    )
    fun findNichtEmailImportierteOhneKostenstellenZuordnung(): List<Beleg>

    @Query(
        "SELECT b FROM Beleg b " +
            "LEFT JOIN FETCH b.lieferant " +
            "LEFT JOIN FETCH b.kostenstelle " +
            "WHERE b.kostenstelle.id = :kostenstelleId " +
            "  AND b.status <> org.example.kalkulationsprogramm.domain.BelegStatus.VERWORFEN " +
            "  AND NOT EXISTS (SELECT a.id FROM BelegKostenstellenAnteil a WHERE a.beleg = b) " +
            "  AND NOT EXISTS (SELECT d.id FROM LieferantDokument d WHERE d.beleg = b) " +
            "ORDER BY b.belegDatum DESC, b.uploadDatum DESC",
    )
    fun findDirektZugeordneteByKostenstelleOhneSplits(
        @Param("kostenstelleId") kostenstelleId: Long?,
    ): List<Beleg>

    fun findTop20ByUploadedByOrderByUploadDatumDesc(uploadedBy: Mitarbeiter): List<Beleg>

    @Query("SELECT b FROM Beleg b WHERE b.status = :status AND b.belegKategorie IN :kategorien ORDER BY b.belegDatum DESC, b.uploadDatum DESC")
    fun findValidierteByKategorien(
        @Param("status") status: BelegStatus,
        @Param("kategorien") kategorien: List<BelegKategorie>,
    ): List<Beleg>

    @Query(
        "SELECT COALESCE(SUM(b.betragBrutto), 0) FROM Beleg b " +
            "WHERE b.status = org.example.kalkulationsprogramm.domain.BelegStatus.VALIDIERT " +
            "  AND b.belegKategorie = :kategorie " +
            "  AND (:von IS NULL OR b.belegDatum >= :von) " +
            "  AND (:bis IS NULL OR b.belegDatum <= :bis)",
    )
    fun summeBruttoByKategorie(
        @Param("kategorie") kategorie: BelegKategorie,
        @Param("von") von: LocalDate?,
        @Param("bis") bis: LocalDate?,
    ): BigDecimal

    fun countByKiAnalyseStatus(status: BelegKiAnalyseStatus): Long

    @Query(
        "SELECT b FROM Beleg b JOIN FETCH b.kostenstelle ks " +
            "WHERE b.status = org.example.kalkulationsprogramm.domain.BelegStatus.VALIDIERT " +
            "  AND ks.istFixkosten = true " +
            "  AND b.belegDatum BETWEEN :von AND :bis " +
            "  AND b.betragBrutto IS NOT NULL",
    )
    fun findValidierteFixkostenBelegeImZeitraum(
        @Param("von") von: LocalDate,
        @Param("bis") bis: LocalDate,
    ): List<Beleg>

    @Query(
        "SELECT b FROM Beleg b LEFT JOIN FETCH b.kostenstelle LEFT JOIN FETCH b.sachkonto " +
            "WHERE b.lieferant.id = :lieferantId " +
            "  AND b.kostenstelle IS NOT NULL " +
            "ORDER BY b.belegDatum DESC, b.id DESC",
    )
    fun findAehnlicheBelegeByLieferant(
        @Param("lieferantId") lieferantId: Long?,
        pageable: Pageable,
    ): List<Beleg>

    @Query(
        "SELECT b FROM Beleg b " +
            "LEFT JOIN FETCH b.lieferant " +
            "LEFT JOIN FETCH b.sachkonto " +
            "WHERE b.status = org.example.kalkulationsprogramm.domain.BelegStatus.VALIDIERT " +
            "  AND (:von IS NULL OR b.belegDatum >= :von) " +
            "  AND (:bis IS NULL OR b.belegDatum <= :bis) " +
            "ORDER BY b.belegDatum ASC, b.id ASC",
    )
    fun findValidierteImZeitraumFuerExport(
        @Param("von") von: LocalDate?,
        @Param("bis") bis: LocalDate?,
    ): List<Beleg>
}
