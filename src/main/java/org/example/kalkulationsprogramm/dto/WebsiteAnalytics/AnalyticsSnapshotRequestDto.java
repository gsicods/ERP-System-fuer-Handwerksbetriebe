package org.example.kalkulationsprogramm.dto.WebsiteAnalytics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Eingehender Tagesschluss-Snapshot der Marketing-Website.
 * Schema-Spezifikation kommt vom Website-Team (siehe ANALYTICS_SNAPSHOT_API.md).
 * <p>
 * Unbekannte Felder werden ignoriert, damit additive Erweiterungen seitens
 * Website nicht direkt eine ERP-Anpassung erfordern (vgl. Section 5 der Spec).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsSnapshotRequestDto {

    @NotNull
    private Integer schemaVersion;

    @NotNull
    private LocalDate snapshotDate;

    @NotNull
    private OffsetDateTime generatedAt;

    @NotNull
    @Valid
    private Totals totals;

    @PositiveOrZero
    private long visitorsToday;

    @PositiveOrZero
    private long visitorsYesterday;

    @Min(0)
    @Max(100)
    private int conversion;

    private List<FunnelStep> funnel;
    private List<TopPage> topPages;
    private List<DeviceCount> devices;
    private List<BrowserCount> browsers;
    private List<CityCount> cities;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Totals {
        @PositiveOrZero
        private long visitors;
        @PositiveOrZero
        private long pageviews;
        @PositiveOrZero
        private long leadsPhone;
        @PositiveOrZero
        private long leadsMail;
        @PositiveOrZero
        private long submissions;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunnelStep {
        private String name;
        private String label;
        private long count;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopPage {
        private String path;
        private long count;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceCount {
        private String device;
        private long count;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrowserCount {
        private String browser;
        private long count;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CityCount {
        private String city;
        private String country;
        private long count;
    }
}
