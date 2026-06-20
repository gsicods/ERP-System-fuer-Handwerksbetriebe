package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.Projekt
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProjektRepository : JpaRepository<Projekt, Long>, JpaSpecificationExecutor<Projekt> {
    @Query("SELECT COUNT(p) FROM Projekt p JOIN p.projektProduktkategorien pk WHERE pk.produktkategorie.id = :kategorieId")
    fun countByProduktkategorieId(@Param("kategorieId") produktkategorieId: Long?): Long

    @Query("SELECT pk.produktkategorie.id, COUNT(DISTINCT p.id) FROM Projekt p JOIN p.projektProduktkategorien pk GROUP BY pk.produktkategorie.id")
    fun getProjectCountsGroupedByKategorie(): List<Array<Any>>

    @Query("SELECT pk.produktkategorie.id, p.id FROM Projekt p JOIN p.projektProduktkategorien pk")
    fun getKategorieProjektPairs(): List<Array<Any>>

    @Query("SELECT p FROM Projekt p JOIN p.projektProduktkategorien pk WHERE pk.produktkategorie.id = :kategorieId")
    fun findByProduktkategorieId(@Param("kategorieId") produktkategorieId: Long?): List<Projekt>

    @Query("SELECT p FROM Projekt p JOIN p.projektProduktkategorien pk WHERE pk.produktkategorie.id = :kategorieId AND p.abschlussdatum BETWEEN :start AND :end")
    fun findByProduktkategorieIdAndAbschlussdatumBetween(
        @Param("kategorieId") kategorieId: Long?,
        @Param("start") start: LocalDate,
        @Param("end") end: LocalDate,
    ): List<Projekt>

    @Query(
        """
                        SELECT DISTINCT p FROM Projekt p
                        LEFT JOIN p.kundenId k
                        LEFT JOIN k.kundenEmails e
                        LEFT JOIN p.kundenEmails pe
                        WHERE LOWER(p.bauvorhaben) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.name) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.kundennummer) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.ansprechspartner) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.telefon) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.mobiltelefon) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.ort) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(e) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(pe) LIKE LOWER(CONCAT('%', :query, '%'))
                        """,
    )
    fun searchByBauvorhabenOrKundeOrEmail(@Param("query") query: String): List<Projekt>

    @Query("SELECT DISTINCT p FROM Projekt p LEFT JOIN FETCH p.zeitbuchungen JOIN p.projektProduktkategorien pk WHERE pk.produktkategorie.id IN :kategorieIds AND p.abgeschlossen = true")
    fun findByProduktkategorieIds(@Param("kategorieIds") kategorieIds: List<Long>): List<Projekt>

    @Query("SELECT DISTINCT p FROM Projekt p LEFT JOIN FETCH p.zeitbuchungen JOIN p.projektProduktkategorien pk WHERE pk.produktkategorie.id IN :kategorieIds AND p.abgeschlossen = true AND p.abschlussdatum BETWEEN :start AND :end")
    fun findByProduktkategorieIdsAndAbschlussdatumBetween(
        @Param("kategorieIds") kategorieIds: List<Long>,
        @Param("start") start: LocalDate,
        @Param("end") end: LocalDate,
    ): List<Projekt>

    override fun findAll(spec: Specification<Projekt>, sort: Sort): List<Projekt>

    @EntityGraph(attributePaths = ["zeitbuchungen"])
    fun findWithZeitenInProjektById(id: Long?): Optional<Projekt>

    @Query("SELECT p.id as id, k.id as kundenIdValue, k.kundenEmails as kundenEmails FROM Projekt p LEFT JOIN p.kundenId k")
    fun findIdsAndKundenEmails(): List<IdEmailOnly>

    @Query("SELECT DISTINCT p FROM Projekt p LEFT JOIN FETCH p.kundenId k LEFT JOIN FETCH k.kundenEmails")
    fun findAllWithKundenEmails(): List<Projekt>

    fun findByKundenId_Id(kundenId: Long?): List<Projekt>

    fun findByAnlegedatumBetween(start: LocalDate, ende: LocalDate): List<Projekt>

    interface IdEmailOnly {
        fun getId(): Long
        fun getKundenIdValue(): Long
        fun getKundenEmails(): List<String>
    }

    @Query("SELECT p.auftragsnummer FROM Projekt p WHERE p.auftragsnummer LIKE CONCAT(:prefix, '%') ORDER BY p.auftragsnummer DESC")
    fun findAuftragsnummernByPrefix(@Param("prefix") prefix: String): List<String>

    @Query("SELECT p.auftragsnummer FROM Projekt p WHERE p.kundenId.id = :kundeId AND p.auftragsnummer LIKE CONCAT(:jahrPrefix, '%')")
    fun findAuftragsnummernByKundeAndYearPrefix(
        @Param("kundeId") kundeId: Long?,
        @Param("jahrPrefix") jahrPrefix: String,
    ): List<String>

    @Query("SELECT p.auftragsnummer FROM Projekt p WHERE p.auftragsnummer LIKE CONCAT(:jahrPrefix, '%')")
    fun findAuftragsnummernByYearPrefix(@Param("jahrPrefix") jahrPrefix: String): List<String>

    fun existsByAuftragsnummer(auftragsnummer: String): Boolean

    @Query("SELECT COUNT(p) > 0 FROM Projekt p WHERE p.auftragsnummer = :auftragsnummer AND p.id != :projektId")
    fun existsByAuftragsnummerAndIdNot(
        @Param("auftragsnummer") auftragsnummer: String,
        @Param("projektId") projektId: Long?,
    ): Boolean

    fun findByBauvorhaben(bauvorhaben: String): Optional<Projekt>

    @Query(
        value = """
                        SELECT DISTINCT p.* FROM projekt p
                        LEFT JOIN kunde k ON p.kunden_id = k.id
                        WHERE EXISTS (SELECT 1 FROM kunden_emails ke WHERE ke.kunden_id = k.id AND lower(ke.email) = lower(:email))
                           OR EXISTS (SELECT 1 FROM projekt_kunden_emails pke WHERE pke.projekt_id = p.id AND lower(pke.email) = lower(:email))
                        """,
        nativeQuery = true,
    )
    fun findByKundenEmail(@Param("email") email: String): List<Projekt>

    interface ProjektSimple {
        fun getId(): Long
        fun getBauvorhaben(): String
        fun getAuftragsnummer(): String
        fun getKunde(): String
        fun isAbgeschlossen(): Boolean
    }

    @Query(
        """
                        SELECT p.id as id, p.bauvorhaben as bauvorhaben, p.auftragsnummer as auftragsnummer, k.name as kunde, p.abgeschlossen as abgeschlossen
                        FROM Projekt p LEFT JOIN p.kundenId k
                        WHERE (:query IS NULL OR :query = ''
                           OR LOWER(p.bauvorhaben) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.name) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.kundennummer) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(k.ansprechspartner) LIKE LOWER(CONCAT('%', :query, '%'))
                           OR LOWER(p.auftragsnummer) LIKE LOWER(CONCAT('%', :query, '%')))
                        ORDER BY p.anlegedatum DESC
                        """,
    )
    fun findSimpleByQuery(@Param("query") query: String?, pageable: Pageable): List<ProjektSimple>
}
