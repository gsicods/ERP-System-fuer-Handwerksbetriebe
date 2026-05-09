package org.example.kalkulationsprogramm.dto.WebsiteAnalytics;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aufbereiteter Snapshot fuer das ERP-Frontend.
 * Listen sind bereits geparst, damit der Client keine doppelte JSON-Decode-Stufe
 * braucht.
 */
@Getter
@Builder
public class AnalyticsSnapshotResponseDto {
    private final int schemaVersion;
    private final LocalDate snapshotDate;
    private final LocalDateTime generatedAt;
    private final LocalDateTime receivedAt;

    private final Totals totals;
    private final long visitorsToday;
    private final long visitorsYesterday;
    private final int conversion;

    private final List<FunnelStep> funnel;
    private final List<TopPage> topPages;
    private final List<DeviceCount> devices;
    private final List<BrowserCount> browsers;
    private final List<CityCount> cities;

    @Getter
    @Builder
    public static class Totals {
        private final long visitors;
        private final long pageviews;
        private final long leadsPhone;
        private final long leadsMail;
        private final long submissions;
    }

    @Getter
    @Builder
    public static class FunnelStep {
        private final String name;
        private final String label;
        private final long count;
    }

    @Getter
    @Builder
    public static class TopPage {
        private final String path;
        private final long count;
    }

    @Getter
    @Builder
    public static class DeviceCount {
        private final String device;
        private final long count;
    }

    @Getter
    @Builder
    public static class BrowserCount {
        private final String browser;
        private final long count;
    }

    @Getter
    @Builder
    public static class CityCount {
        private final String city;
        private final String country;
        private final long count;
    }
}
