package org.example.kalkulationsprogramm.controller;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.dto.WebsiteAnalytics.AnalyticsSnapshotResponseDto;
import org.example.kalkulationsprogramm.service.WebsiteAnalyticsSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Lese-Endpoint fuer das ERP-Frontend (Erfolgsanalyse-Seite).
 * Liefert den juengsten gespeicherten Website-Snapshot. Liegt noch keiner
 * vor, antwortet der Endpoint mit 204.
 */
@RestController
@RequestMapping("/api/website-analytics")
@RequiredArgsConstructor
public class WebsiteAnalyticsSnapshotController {

    private final WebsiteAnalyticsSnapshotService service;

    @GetMapping("/latest")
    public ResponseEntity<AnalyticsSnapshotResponseDto> latest() {
        Optional<AnalyticsSnapshotResponseDto> latest = service.findLatest();
        return latest.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
