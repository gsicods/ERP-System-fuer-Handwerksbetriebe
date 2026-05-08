package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.SvSatz;
import org.example.kalkulationsprogramm.domain.SvSatzTyp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SvSatzRepository extends JpaRepository<SvSatz, Long> {

    List<SvSatz> findAllByOrderBySatzTypAscGueltigAbDesc();

    /**
     * Liefert den zum Stichtag aktiven Satz fuer den Typ (juengster Eintrag mit
     * gueltig_ab &lt;= stichtag).
     */
    Optional<SvSatz> findFirstBySatzTypAndGueltigAbLessThanEqualOrderByGueltigAbDesc(
            SvSatzTyp typ, LocalDate stichtag);
}
