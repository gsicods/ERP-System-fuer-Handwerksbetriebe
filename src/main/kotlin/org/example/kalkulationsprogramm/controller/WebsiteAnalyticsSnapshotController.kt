package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.WebsiteAnalytics.AnalyticsSnapshotResponseDto
import org.example.kalkulationsprogramm.service.WebsiteAnalyticsSnapshotService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/website-analytics")
class WebsiteAnalyticsSnapshotController(private val service: WebsiteAnalyticsSnapshotService) {
    @GetMapping("/latest")
    fun latest(): ResponseEntity<AnalyticsSnapshotResponseDto> =
        service.findLatest().map { ResponseEntity.ok(it) }.orElseGet { ResponseEntity.noContent().build() }
}
