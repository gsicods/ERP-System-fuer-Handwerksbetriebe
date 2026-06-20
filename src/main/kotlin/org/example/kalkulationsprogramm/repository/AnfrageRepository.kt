package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import org.example.kalkulationsprogramm.domain.Anfrage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AnfrageRepository : JpaRepository<Anfrage, Long> {
    @Query("SELECT DISTINCT a FROM Anfrage a LEFT JOIN FETCH a.kunde k LEFT JOIN FETCH k.kundenEmails WHERE a.projekt.id IN :ids")
    fun findByProjektIdIn(ids: List<Long>): List<Anfrage>

    @Query("SELECT DISTINCT a FROM Anfrage a LEFT JOIN FETCH a.kunde k LEFT JOIN FETCH k.kundenEmails WHERE a.kunde.id = :kundeId")
    fun findByKundeId(@Param("kundeId") kundeId: Long?): List<Anfrage>

    @Query("SELECT DISTINCT a FROM Anfrage a LEFT JOIN FETCH a.kunde k LEFT JOIN FETCH k.kundenEmails WHERE LOWER(k.kundennummer) = LOWER(:kundennummer)")
    fun findByKunde_KundennummerIgnoreCase(@Param("kundennummer") kundennummer: String): List<Anfrage>

    @Query("select a.projekt.id, g.dokumentid from Anfrage a join a.dokumente g where a.projekt.id in :ids and g.geschaeftsdokumentart = 'Anfrage'")
    fun findDokumentIdsByProjektIds(ids: List<Long>): List<Array<Any>>

    @Query("SELECT DISTINCT a FROM Anfrage a LEFT JOIN FETCH a.kunde k LEFT JOIN FETCH k.kundenEmails")
    fun findAllWithKundenEmails(): List<Anfrage>

    @Query(
        """
               SELECT DISTINCT a FROM Anfrage a
               LEFT JOIN FETCH a.kunde k
               LEFT JOIN k.kundenEmails e
               WHERE (
                      :kundenname IS NULL OR
                      LOWER(k.name) LIKE CONCAT('%', LOWER(:kundenname), '%') OR
                      LOWER(k.kundennummer) LIKE CONCAT('%', LOWER(:kundenname), '%') OR
                      LOWER(k.ansprechspartner) LIKE CONCAT('%', LOWER(:kundenname), '%') OR
                      LOWER(k.telefon) LIKE CONCAT('%', LOWER(:kundenname), '%') OR
                      LOWER(k.mobiltelefon) LIKE CONCAT('%', LOWER(:kundenname), '%') OR
                      LOWER(k.ort) LIKE CONCAT('%', LOWER(:kundenname), '%') OR
                      LOWER(e) LIKE CONCAT('%', LOWER(:kundenname), '%')
                 )
                 AND (
                      :bauvorhaben IS NULL OR
                      LOWER(a.bauvorhaben) LIKE CONCAT('%', LOWER(:bauvorhaben), '%')
                 )
                 AND (:startDate IS NULL OR a.anlegedatum >= :startDate)
                 AND (:endDate IS NULL OR a.anlegedatum <= :endDate)
                 AND (
                      :anfragesnummer IS NULL OR EXISTS (
                          SELECT d FROM AusgangsGeschaeftsDokument d
                          WHERE d.anfrage = a
                            AND d.typ = 'ANFRAGE'
                            AND LOWER(d.dokumentNummer) LIKE LOWER(CONCAT('%', :anfragesnummer, '%'))
                      )
                 )
               """,
    )
    fun search(
        kundenname: String?,
        bauvorhaben: String?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        anfragesnummer: String?,
    ): List<Anfrage>

    fun findByAnlegedatumBetween(startDatum: LocalDate, endDatum: LocalDate): List<Anfrage>

    @Query(
        """
               SELECT DISTINCT function('YEAR', a.anlegedatum)
               FROM Anfrage a
               WHERE a.anlegedatum IS NOT NULL
               ORDER BY function('YEAR', a.anlegedatum) DESC
               """,
    )
    fun findDistinctAnlegedatumJahre(): List<Int>

    @Query(
        value = """
               SELECT DISTINCT a.* FROM anfrage a
               LEFT JOIN kunde k ON a.kunde_id = k.id
               WHERE EXISTS (SELECT 1 FROM anfrage_kunden_emails ake WHERE ake.anfrage_id = a.id AND lower(ake.email) = lower(:email))
                  OR EXISTS (SELECT 1 FROM kunden_emails ke WHERE ke.kunden_id = k.id AND lower(ke.email) = lower(:email))
               """,
        nativeQuery = true,
    )
    fun findByKundenEmail(@Param("email") email: String): List<Anfrage>

    @Query(
        """
               SELECT DISTINCT a FROM Anfrage a
               LEFT JOIN FETCH a.kunde k
               JOIN a.notizen n
               JOIN n.mitarbeiter m
               WHERE a.projekt IS NULL
                 AND a.abgeschlossen = false
                 AND m.loginToken = :token
               ORDER BY a.createdAt DESC
               """,
    )
    fun findOffeneFunnelAnfragen(@Param("token") token: String): List<Anfrage>

    @Query(
        """
               SELECT DISTINCT a FROM Anfrage a
               LEFT JOIN FETCH a.kunde k
               LEFT JOIN k.kundenEmails e
               LEFT JOIN a.kundenEmails ae
               WHERE LOWER(a.bauvorhaben) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(k.name) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(k.kundennummer) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(k.ansprechspartner) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(k.telefon) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(k.mobiltelefon) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(k.ort) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(e) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(ae) LIKE LOWER(CONCAT('%', :query, '%'))
               """,
    )
    fun searchByBauvorhabenOrKundeOrEmail(@Param("query") query: String): List<Anfrage>
}
