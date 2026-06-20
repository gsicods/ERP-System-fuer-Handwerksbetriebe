package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.WebsiteAnalyticsSnapshot
import org.springframework.data.jpa.repository.JpaRepository

interface WebsiteAnalyticsSnapshotRepository : JpaRepository<WebsiteAnalyticsSnapshot, Long> {
    fun findBySnapshotDate(snapshotDate: LocalDate): Optional<WebsiteAnalyticsSnapshot>

    fun findFirstByOrderBySnapshotDateDesc(): Optional<WebsiteAnalyticsSnapshot>
}
