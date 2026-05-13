package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.BelegKostenstellenAnteil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BelegKostenstellenAnteilRepository extends JpaRepository<BelegKostenstellenAnteil, Long> {

    @Query("SELECT a FROM BelegKostenstellenAnteil a WHERE a.beleg.id = :belegId ORDER BY a.id ASC")
    List<BelegKostenstellenAnteil> findByBelegId(@Param("belegId") Long belegId);

    @Modifying
    @Query("DELETE FROM BelegKostenstellenAnteil a WHERE a.beleg.id = :belegId")
    void deleteByBelegId(@Param("belegId") Long belegId);

    /**
     * Alle Anteile auf einer bestimmten Kostenstelle, deren Streckungs-Zeitraum
     * das gegebene Jahr enthaelt. Wird vom Verrechnungslohn-Rechner verwendet,
     * um auch gestreckte Bar-Belege in die Gemeinkosten einzubeziehen.
     */
    @Query("SELECT a FROM BelegKostenstellenAnteil a "
            + "WHERE a.kostenstelle.id = :kostenstelleId "
            + "AND (a.streckungStartJahr IS NULL OR a.streckungStartJahr <= :jahr) "
            + "AND (a.streckungStartJahr IS NULL OR a.streckungJahre IS NULL "
            + "     OR a.streckungJahre <= 1 OR :jahr < a.streckungStartJahr + a.streckungJahre)")
    List<BelegKostenstellenAnteil> findAktiveByKostenstelleUndJahr(
            @Param("kostenstelleId") Long kostenstelleId,
            @Param("jahr") int jahr);
}
