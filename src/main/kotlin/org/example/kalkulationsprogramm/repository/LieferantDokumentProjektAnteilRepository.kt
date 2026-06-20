package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import org.example.kalkulationsprogramm.domain.LieferantDokumentProjektAnteil
import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LieferantDokumentProjektAnteilRepository : JpaRepository<LieferantDokumentProjektAnteil, Long> {
    fun findByDokumentId(dokumentId: Long?): List<LieferantDokumentProjektAnteil>

    @Query(
        "SELECT pa FROM LieferantDokumentProjektAnteil pa " +
            "LEFT JOIN FETCH pa.projekt " +
            "LEFT JOIN FETCH pa.kostenstelle " +
            "LEFT JOIN FETCH pa.zugeordnetVon " +
            "WHERE pa.dokument.id = :dokumentId",
    )
    fun findByDokumentIdEager(@Param("dokumentId") dokumentId: Long?): List<LieferantDokumentProjektAnteil>

    fun findByProjektId(projektId: Long?): List<LieferantDokumentProjektAnteil>

    @Query(
        "SELECT pa FROM LieferantDokumentProjektAnteil pa " +
            "LEFT JOIN FETCH pa.dokument d " +
            "LEFT JOIN FETCH d.geschaeftsdaten " +
            "LEFT JOIN FETCH d.lieferant " +
            "LEFT JOIN FETCH d.attachment a " +
            "LEFT JOIN FETCH a.email " +
            "LEFT JOIN FETCH pa.zugeordnetVon " +
            "WHERE pa.projekt.id = :projektId",
    )
    fun findByProjektIdEager(@Param("projektId") projektId: Long?): List<LieferantDokumentProjektAnteil>

    @Query("SELECT COALESCE(SUM(pa.berechneterBetrag), 0) FROM LieferantDokumentProjektAnteil pa WHERE pa.projekt.id = :projektId")
    fun sumBerechneterBetragByProjektId(@Param("projektId") projektId: Long?): BigDecimal

    @Query(
        "SELECT pa FROM LieferantDokumentProjektAnteil pa " +
            "WHERE pa.projekt.id = :projektId AND pa.dokument.typ = :typ " +
            "ORDER BY pa.dokument.uploadDatum DESC",
    )
    fun findByProjektIdAndDokumentTyp(
        @Param("projektId") projektId: Long?,
        @Param("typ") typ: LieferantDokumentTyp,
    ): List<LieferantDokumentProjektAnteil>

    fun findByKostenstelleId(kostenstelleId: Long?): List<LieferantDokumentProjektAnteil>
}
