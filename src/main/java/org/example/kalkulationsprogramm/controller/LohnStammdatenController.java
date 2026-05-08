package org.example.kalkulationsprogramm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.dto.GewerkDto;
import org.example.kalkulationsprogramm.dto.KrankenkasseDto;
import org.example.kalkulationsprogramm.dto.SvSatzDto;
import org.example.kalkulationsprogramm.service.GewerkService;
import org.example.kalkulationsprogramm.service.KrankenkasseService;
import org.example.kalkulationsprogramm.service.SvSatzService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Stammdaten fuer Lohn-/Sozialversicherungs-Berechnung:
 *   - Krankenkassen (mit Zusatzbeitrag)
 *   - SV-Saetze (KV/PV/RV/AV/Minijob/Umlagen)
 *   - Gewerke + zugeordnete Berufsgenossenschaft (Unfallversicherung)
 *
 * Alle drei sind im Frontend editierbar - die Migrationen liefern nur den
 * Initial-Seed.
 */
@RestController
@RequestMapping("/api/lohn-stammdaten")
@RequiredArgsConstructor
public class LohnStammdatenController {

    private final KrankenkasseService krankenkasseService;
    private final SvSatzService svSatzService;
    private final GewerkService gewerkService;

    // ==================== KRANKENKASSEN ====================

    @GetMapping("/krankenkassen")
    public ResponseEntity<List<KrankenkasseDto>> getKrankenkassen(
            @RequestParam(name = "nurAktive", required = false, defaultValue = "false") boolean nurAktive) {
        return ResponseEntity.ok(nurAktive ? krankenkasseService.findAktiv() : krankenkasseService.findAll());
    }

    @PostMapping("/krankenkassen")
    public ResponseEntity<?> erstelleKrankenkasse(@Valid @RequestBody KrankenkasseDto dto) {
        try {
            dto.setId(null);
            return ResponseEntity.ok(krankenkasseService.save(dto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/krankenkassen/{id}")
    public ResponseEntity<?> aktualisiereKrankenkasse(@PathVariable Long id, @Valid @RequestBody KrankenkasseDto dto) {
        try {
            dto.setId(id);
            return ResponseEntity.ok(krankenkasseService.save(dto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/krankenkassen/{id}")
    public ResponseEntity<Void> loescheKrankenkasse(@PathVariable Long id) {
        krankenkasseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== SV-SAETZE ====================

    @GetMapping("/sv-saetze")
    public ResponseEntity<List<SvSatzDto>> getSvSaetze() {
        return ResponseEntity.ok(svSatzService.findAll());
    }

    @PostMapping("/sv-saetze")
    public ResponseEntity<?> erstelleSvSatz(@Valid @RequestBody SvSatzDto dto) {
        try {
            dto.setId(null);
            return ResponseEntity.ok(svSatzService.save(dto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/sv-saetze/{id}")
    public ResponseEntity<?> aktualisiereSvSatz(@PathVariable Long id, @Valid @RequestBody SvSatzDto dto) {
        try {
            dto.setId(id);
            return ResponseEntity.ok(svSatzService.save(dto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/sv-saetze/{id}")
    public ResponseEntity<Void> loescheSvSatz(@PathVariable Long id) {
        svSatzService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== GEWERKE ====================

    @GetMapping("/gewerke")
    public ResponseEntity<List<GewerkDto>> getGewerke(
            @RequestParam(name = "nurAktive", required = false, defaultValue = "false") boolean nurAktive) {
        return ResponseEntity.ok(nurAktive ? gewerkService.findAktiv() : gewerkService.findAll());
    }

    @PostMapping("/gewerke")
    public ResponseEntity<?> erstelleGewerk(@Valid @RequestBody GewerkDto dto) {
        try {
            dto.setId(null);
            return ResponseEntity.ok(gewerkService.save(dto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/gewerke/{id}")
    public ResponseEntity<?> aktualisiereGewerk(@PathVariable Long id, @Valid @RequestBody GewerkDto dto) {
        try {
            dto.setId(id);
            return ResponseEntity.ok(gewerkService.save(dto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/gewerke/{id}")
    public ResponseEntity<Void> loescheGewerk(@PathVariable Long id) {
        gewerkService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
