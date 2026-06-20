package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AusgangsGeschaeftsDokumentRepository : JpaRepository<AusgangsGeschaeftsDokument, Long> {
    fun findByProjektIdOrderByDatumDesc(projektId: Long?): List<AusgangsGeschaeftsDokument>

    fun existsByProjektId(projektId: Long?): Boolean

    fun findByAnfrageIdOrderByDatumDesc(anfrageId: Long?): List<AusgangsGeschaeftsDokument>

    fun findByKundeIdOrderByDatumDesc(kundeId: Long?): List<AusgangsGeschaeftsDokument>

    fun findByDokumentNummer(dokumentNummer: String): Optional<AusgangsGeschaeftsDokument>

    fun findByVorgaengerIdOrderByErstelltAmAsc(vorgaengerId: Long?): List<AusgangsGeschaeftsDokument>

    @Query("SELECT COUNT(d) FROM AusgangsGeschaeftsDokument d WHERE d.vorgaenger.id = :vorgaengerId AND d.typ = :typ")
    fun countByVorgaengerIdAndTyp(vorgaengerId: Long?, typ: AusgangsGeschaeftsDokumentTyp): Int

    fun findFirstByAnfrageIdAndTyp(
        anfrageId: Long?,
        typ: AusgangsGeschaeftsDokumentTyp,
    ): Optional<AusgangsGeschaeftsDokument>

    fun existsByProjektIdAndVorgaengerIsNull(projektId: Long?): Boolean

    fun existsByAnfrageIdAndVorgaengerIsNull(anfrageId: Long?): Boolean

    fun existsByProjektIdAndVorgaengerIsNullAndTyp(
        projektId: Long?,
        typ: AusgangsGeschaeftsDokumentTyp,
    ): Boolean

    fun existsByAnfrageIdAndVorgaengerIsNullAndTyp(
        anfrageId: Long?,
        typ: AusgangsGeschaeftsDokumentTyp,
    ): Boolean

    @Query("SELECT d FROM AusgangsGeschaeftsDokument d WHERE d.anfrage.projekt.id = :projektId AND d.projekt IS NULL")
    fun findByAnfrageProjektIdAndProjektIsNull(projektId: Long?): List<AusgangsGeschaeftsDokument>

    @Query("SELECT MAX(d.dokumentNummer) FROM AusgangsGeschaeftsDokument d WHERE d.dokumentNummer LIKE :prefix%")
    fun findMaxDokumentNummerByPrefix(prefix: String): Optional<String>

    fun findByDatumBetweenOrderByDatumDesc(start: LocalDate, end: LocalDate): List<AusgangsGeschaeftsDokument>

    fun findAllByOrderByDatumDesc(): List<AusgangsGeschaeftsDokument>

    @Query("SELECT d.id, d.anfrage.id FROM AusgangsGeschaeftsDokument d WHERE d.anfrage.id IN :anfrageIds")
    fun findIdAnfrageIdMappingByAnfrageIds(
        @Param("anfrageIds") anfrageIds: List<Long>,
    ): List<Array<Any>>

    @Query("SELECT d.id, d.projekt.id FROM AusgangsGeschaeftsDokument d WHERE d.projekt.id IN :projektIds")
    fun findIdProjektIdMappingByProjektIds(
        @Param("projektIds") projektIds: List<Long>,
    ): List<Array<Any>>

    @Query(
        "SELECT d.rechnungsadresseOverride FROM AusgangsGeschaeftsDokument d " +
            "LEFT JOIN d.projekt p LEFT JOIN d.anfrage a LEFT JOIN a.projekt ap " +
            "WHERE (p.id = :projektId OR ap.id = :projektId) " +
            "AND d.rechnungsadresseOverride IS NOT NULL AND TRIM(d.rechnungsadresseOverride) <> '' " +
            "ORDER BY d.geaendertAm DESC",
    )
    fun findRechnungsadresseOverridesByProjektId(
        @Param("projektId") projektId: Long?,
    ): List<String>

    @Query(
        "SELECT d.rechnungsadresseOverride FROM AusgangsGeschaeftsDokument d " +
            "WHERE d.anfrage.id = :anfrageId " +
            "AND d.rechnungsadresseOverride IS NOT NULL AND TRIM(d.rechnungsadresseOverride) <> '' " +
            "ORDER BY d.geaendertAm DESC",
    )
    fun findRechnungsadresseOverridesByAnfrageId(
        @Param("anfrageId") anfrageId: Long?,
    ): List<String>
}
