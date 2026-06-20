package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.BelegKostenstellenAnteil
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BelegKostenstellenAnteilRepository : JpaRepository<BelegKostenstellenAnteil, Long> {
    @Query("SELECT a FROM BelegKostenstellenAnteil a WHERE a.beleg.id = :belegId ORDER BY a.id ASC")
    fun findByBelegId(@Param("belegId") belegId: Long?): List<BelegKostenstellenAnteil>

    @Query(
        "SELECT a FROM BelegKostenstellenAnteil a " +
            "JOIN FETCH a.beleg b " +
            "JOIN FETCH a.kostenstelle ks " +
            "LEFT JOIN FETCH a.zugeordnetVon " +
            "WHERE ks.id = :kostenstelleId " +
            "AND b.status <> org.example.kalkulationsprogramm.domain.BelegStatus.VERWORFEN " +
            "AND NOT EXISTS (SELECT d.id FROM LieferantDokument d WHERE d.beleg = b) " +
            "ORDER BY b.belegDatum DESC, b.uploadDatum DESC, a.id DESC",
    )
    fun findByKostenstelleIdEager(
        @Param("kostenstelleId") kostenstelleId: Long?,
    ): List<BelegKostenstellenAnteil>

    @Modifying
    @Query("DELETE FROM BelegKostenstellenAnteil a WHERE a.beleg.id = :belegId")
    fun deleteByBelegId(@Param("belegId") belegId: Long?)

    @Query(
        "SELECT a FROM BelegKostenstellenAnteil a " +
            "WHERE a.kostenstelle.id = :kostenstelleId " +
            "AND (a.streckungStartJahr IS NULL OR a.streckungStartJahr <= :jahr) " +
            "AND (a.streckungStartJahr IS NULL OR a.streckungJahre IS NULL " +
            "     OR a.streckungJahre <= 1 OR :jahr < a.streckungStartJahr + a.streckungJahre)",
    )
    fun findAktiveByKostenstelleUndJahr(
        @Param("kostenstelleId") kostenstelleId: Long?,
        @Param("jahr") jahr: Int,
    ): List<BelegKostenstellenAnteil>

    @Query(
        "SELECT a FROM BelegKostenstellenAnteil a " +
            "JOIN FETCH a.kostenstelle ks " +
            "JOIN FETCH a.beleg b " +
            "WHERE b.status = org.example.kalkulationsprogramm.domain.BelegStatus.VALIDIERT " +
            "AND ks.istFixkosten = true " +
            "AND (" +
            "  ((a.streckungJahre IS NULL OR a.streckungJahre <= 1) " +
            "   AND (a.streckungStartJahr IS NULL OR a.streckungStartJahr = :jahr))" +
            "  OR " +
            "  (a.streckungJahre > 1 AND a.streckungStartJahr IS NOT NULL " +
            "   AND :jahr >= a.streckungStartJahr " +
            "   AND :jahr < a.streckungStartJahr + a.streckungJahre)" +
            ")",
    )
    fun findAktiveFixkostenAnteileImJahr(@Param("jahr") jahr: Int): List<BelegKostenstellenAnteil>
}
