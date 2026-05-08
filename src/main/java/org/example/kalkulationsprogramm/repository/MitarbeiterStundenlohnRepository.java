package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.MitarbeiterStundenlohn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MitarbeiterStundenlohnRepository extends JpaRepository<MitarbeiterStundenlohn, Long> {

    List<MitarbeiterStundenlohn> findByMitarbeiterIdOrderByGueltigAbDesc(Long mitarbeiterId);

    Optional<MitarbeiterStundenlohn> findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(
            Long mitarbeiterId, LocalDate stichtag);
}
