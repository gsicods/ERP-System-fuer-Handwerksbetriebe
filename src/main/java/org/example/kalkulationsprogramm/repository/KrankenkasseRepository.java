package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.Krankenkasse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KrankenkasseRepository extends JpaRepository<Krankenkasse, Long> {
    List<Krankenkasse> findAllByOrderByNameAsc();
    List<Krankenkasse> findByAktivTrueOrderByNameAsc();
}
