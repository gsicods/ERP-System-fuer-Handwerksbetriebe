package org.example.kalkulationsprogramm.dto.WebsiteAnalytics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnalyticsSnapshotRequestDto(
    @field:NotNull var schemaVersion: Int? = null,
    @field:NotNull var snapshotDate: LocalDate? = null,
    @field:NotNull var generatedAt: OffsetDateTime? = null,
    @field:NotNull @field:Valid var totals: Totals? = null,
    @field:PositiveOrZero var visitorsToday: Long = 0,
    @field:PositiveOrZero var visitorsYesterday: Long = 0,
    @field:Min(0) @field:Max(100) var conversion: Int = 0,
    var funnel: List<FunnelStep>? = null,
    var topPages: List<TopPage>? = null,
    var devices: List<DeviceCount>? = null,
    var browsers: List<BrowserCount>? = null,
    var cities: List<CityCount>? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Totals(@field:PositiveOrZero var visitors: Long = 0, @field:PositiveOrZero var pageviews: Long = 0, @field:PositiveOrZero var leadsPhone: Long = 0, @field:PositiveOrZero var leadsMail: Long = 0, @field:PositiveOrZero var submissions: Long = 0)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class FunnelStep(var name: String? = null, var label: String? = null, var count: Long = 0)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TopPage(var path: String? = null, var count: Long = 0)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DeviceCount(var device: String? = null, var count: Long = 0)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class BrowserCount(var browser: String? = null, var count: Long = 0)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CityCount(var city: String? = null, var country: String? = null, var count: Long = 0)
}
