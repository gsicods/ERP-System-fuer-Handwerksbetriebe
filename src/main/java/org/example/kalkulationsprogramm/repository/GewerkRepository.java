package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.Gewerk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GewerkRepository extends JpaRepository<Gewerk, Long> {
    List<Gewerk> findAllByOrderByNameAsc();
    List<Gewerk> findByAktivTrueOrderByNameAsc();
}
