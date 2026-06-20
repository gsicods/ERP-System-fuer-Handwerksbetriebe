package org.example.kalkulationsprogramm.repository

import java.time.LocalDateTime
import java.util.Optional
import org.example.kalkulationsprogramm.domain.LieferantDokument
import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LieferantDokumentRepository : JpaRepository<LieferantDokument, Long> {
    @Query(
        "SELECT d FROM LieferantDokument d " +
            "LEFT JOIN FETCH d.geschaeftsdaten " +
            "LEFT JOIN FETCH d.uploadedBy " +
            "LEFT JOIN FETCH d.lieferant " +
            "WHERE d.lieferant.id = :lieferantId " +
            "ORDER BY d.uploadDatum DESC",
    )
    fun findByLieferantIdOrderByUploadDatumDesc(
        @Param("lieferantId") lieferantId: Long?,
    ): List<LieferantDokument>

    @Query(
        "SELECT d FROM LieferantDokument d " +
            "LEFT JOIN FETCH d.geschaeftsdaten " +
            "LEFT JOIN FETCH d.uploadedBy " +
            "LEFT JOIN FETCH d.lieferant " +
            "WHERE d.lieferant.id = :lieferantId AND d.typ = :typ " +
            "ORDER BY d.uploadDatum DESC",
    )
    fun findByLieferantIdAndTypOrderByUploadDatumDesc(
        @Param("lieferantId") lieferantId: Long?,
        @Param("typ") typ: LieferantDokumentTyp,
    ): List<LieferantDokument>

    @Query(
        "SELECT d FROM LieferantDokument d " +
            "LEFT JOIN FETCH d.geschaeftsdaten " +
            "LEFT JOIN FETCH d.uploadedBy " +
            "LEFT JOIN FETCH d.lieferant " +
            "WHERE d.lieferant.id = :lieferantId AND d.typ IN :typen " +
            "ORDER BY d.uploadDatum DESC",
    )
    fun findByLieferantIdAndTypIn(
        @Param("lieferantId") lieferantId: Long?,
        @Param("typen") typen: List<LieferantDokumentTyp>,
    ): List<LieferantDokument>

    @Query("SELECT DISTINCT d FROM LieferantDokument d JOIN d.projektAnteile pa WHERE pa.projekt.id = :projektId ORDER BY d.uploadDatum DESC")
    fun findByProjektId(@Param("projektId") projektId: Long?): List<LieferantDokument>

    @Query("SELECT DISTINCT d FROM LieferantDokument d JOIN d.projektAnteile pa WHERE pa.projekt.id = :projektId AND d.typ = :typ ORDER BY d.uploadDatum DESC")
    fun findByProjektIdAndTyp(
        @Param("projektId") projektId: Long?,
        @Param("typ") typ: LieferantDokumentTyp,
    ): List<LieferantDokument>

    fun existsByLieferantIdAndOriginalDateinameContaining(lieferantId: Long?, filenameFragment: String): Boolean

    @Query(
        "SELECT DISTINCT d FROM LieferantDokument d " +
            "LEFT JOIN FETCH d.geschaeftsdaten g " +
            "LEFT JOIN FETCH d.attachment a " +
            "WHERE d.lieferant.id = :lieferantId " +
            "AND d.typ = org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.LIEFERSCHEIN " +
            "AND (LOWER(g.dokumentNummer) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.originalDateiname) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(a.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY d.uploadDatum DESC",
    )
    fun searchLieferscheine(
        @Param("lieferantId") lieferantId: Long?,
        @Param("query") query: String,
    ): List<LieferantDokument>

    @Query("SELECT d FROM LieferantDokument d WHERE d.typ = org.example.kalkulationsprogramm.domain.LieferantDokumentTyp.LIEFERSCHEIN AND d.uploadDatum >= :since ORDER BY d.uploadDatum DESC")
    fun findRecentLieferscheine(@Param("since") since: LocalDateTime): List<LieferantDokument>

    @Query(
        "SELECT d FROM LieferantDokument d " +
            "LEFT JOIN FETCH d.geschaeftsdaten " +
            "WHERE d.beleg.id = :belegId",
    )
    fun findByBelegId(@Param("belegId") belegId: Long?): Optional<LieferantDokument>

    @Query(
        "SELECT d FROM LieferantDokument d " +
            "LEFT JOIN FETCH d.geschaeftsdaten " +
            "WHERE d.attachment IS NULL " +
            "AND LOWER(d.gespeicherterDateiname) LIKE '%.xml'",
    )
    fun findMitXmlAnzeigedatei(): List<LieferantDokument>
}
