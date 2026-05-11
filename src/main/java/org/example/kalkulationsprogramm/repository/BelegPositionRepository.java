package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.BelegPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BelegPositionRepository extends JpaRepository<BelegPosition, Long> {

    List<BelegPosition> findByBelegIdOrderBySortierungAsc(Long belegId);

    void deleteByBelegId(Long belegId);
}
