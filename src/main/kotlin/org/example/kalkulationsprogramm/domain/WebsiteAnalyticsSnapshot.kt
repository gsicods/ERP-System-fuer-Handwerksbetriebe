package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "website_analytics_snapshot",
    uniqueConstraints = [UniqueConstraint(name = "uk_website_analytics_snapshot_date", columnNames = ["snapshot_date"])]
)
open class WebsiteAnalyticsSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(name = "snapshot_date", nullable = false)
    open var snapshotDate: LocalDate? = null

    @Column(name = "schema_version", nullable = false)
    open var schemaVersion: Int = 0

    @Column(name = "generated_at", nullable = false)
    open var generatedAt: LocalDateTime? = null

    @Column(name = "received_at", nullable = false)
    open var receivedAt: LocalDateTime? = null

    @Column(name = "totals_visitors", nullable = false)
    open var totalsVisitors: Long = 0

    @Column(name = "totals_pageviews", nullable = false)
    open var totalsPageviews: Long = 0

    @Column(name = "totals_leads_phone", nullable = false)
    open var totalsLeadsPhone: Long = 0

    @Column(name = "totals_leads_mail", nullable = false)
    open var totalsLeadsMail: Long = 0

    @Column(name = "totals_submissions", nullable = false)
    open var totalsSubmissions: Long = 0

    @Column(name = "visitors_today", nullable = false)
    open var visitorsToday: Long = 0

    @Column(name = "visitors_yesterday", nullable = false)
    open var visitorsYesterday: Long = 0

    @Column(name = "conversion", nullable = false)
    open var conversion: Int = 0

    @Column(name = "funnel_json", columnDefinition = "LONGTEXT")
    open var funnelJson: String? = null

    @Column(name = "top_pages_json", columnDefinition = "LONGTEXT")
    open var topPagesJson: String? = null

    @Column(name = "devices_json", columnDefinition = "LONGTEXT")
    open var devicesJson: String? = null

    @Column(name = "browsers_json", columnDefinition = "LONGTEXT")
    open var browsersJson: String? = null

    @Column(name = "cities_json", columnDefinition = "LONGTEXT")
    open var citiesJson: String? = null

    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    open var rawPayload: String? = null

    @PrePersist
    open fun onPersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now(ZoneOffset.UTC)
        }
    }
}
