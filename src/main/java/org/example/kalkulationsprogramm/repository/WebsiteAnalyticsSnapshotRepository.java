package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.WebsiteAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WebsiteAnalyticsSnapshotRepository extends JpaRepository<WebsiteAnalyticsSnapshot, Long> {

    Optional<WebsiteAnalyticsSnapshot> findBySnapshotDate(LocalDate snapshotDate);

    Optional<WebsiteAnalyticsSnapshot> findFirstByOrderBySnapshotDateDesc();
}
