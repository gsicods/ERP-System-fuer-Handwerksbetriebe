package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.Zahlungsart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZahlungsartRepository extends JpaRepository<Zahlungsart, Long> {
    List<Zahlungsart> findAllByOrderBySortierungAscBezeichnungAsc();
    List<Zahlungsart> findByAktivTrueOrderBySortierungAscBezeichnungAsc();
}
