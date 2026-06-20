package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.AbteilungDokumentBerechtigung
import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AbteilungDokumentBerechtigungRepository : JpaRepository<AbteilungDokumentBerechtigung, Long> {
    fun findByAbteilungId(abteilungId: Long?): List<AbteilungDokumentBerechtigung>

    fun findByAbteilungIdAndDokumentTyp(
        abteilungId: Long?,
        dokumentTyp: LieferantDokumentTyp,
    ): Optional<AbteilungDokumentBerechtigung>

    @Query(
        value = "SELECT dokument_typ FROM abteilung_dokument_berechtigung WHERE abteilung_id = :abteilungId AND darf_sehen = true",
        nativeQuery = true,
    )
    fun findSichtbareTypenByAbteilungId(@Param("abteilungId") abteilungId: Long?): List<String>

    @Query(
        value = "SELECT dokument_typ FROM abteilung_dokument_berechtigung WHERE abteilung_id = :abteilungId AND darf_scannen = true",
        nativeQuery = true,
    )
    fun findScanbarTypenByAbteilungId(@Param("abteilungId") abteilungId: Long?): List<String>

    @Query(
        value = "SELECT dokument_typ FROM abteilung_dokument_berechtigung WHERE abteilung_id IN :abteilungIds AND darf_sehen = true",
        nativeQuery = true,
    )
    fun findSichtbareTypenByAbteilungIds(@Param("abteilungIds") abteilungIds: List<Long>): List<String>

    @Query(
        value = "SELECT dokument_typ FROM abteilung_dokument_berechtigung WHERE abteilung_id IN :abteilungIds AND darf_scannen = true",
        nativeQuery = true,
    )
    fun findScanbarTypenByAbteilungIds(@Param("abteilungIds") abteilungIds: List<Long>): List<String>
}
