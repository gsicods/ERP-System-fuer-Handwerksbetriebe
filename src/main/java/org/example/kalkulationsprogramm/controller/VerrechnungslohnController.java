package org.example.kalkulationsprogramm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.dto.Verrechnungslohn.VerrechnungslohnErgebnisDto;
import org.example.kalkulationsprogramm.dto.Verrechnungslohn.VerrechnungslohnUebernehmenRequest;
import org.example.kalkulationsprogramm.service.VerrechnungslohnService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints fuer den Verrechnungslohn-Rechner ("Was muss meine Stunde kosten?").
 *
 * GET /api/verrechnungslohn?jahr=2026   - liefert die Vorschau (Loehne, Stunden,
 *                                          Gemeinkosten, Selbstkosten/Stunde).
 * POST /api/verrechnungslohn/uebernehmen - schreibt den errechneten Satz auf alle
 *                                          Arbeitsgaenge fuer das Jahr.
 *                                          Nur fuer ADMIN (siehe SecurityConfig).
 */
@RestController
@RequestMapping("/api/verrechnungslohn")
@RequiredArgsConstructor
@Validated
public class VerrechnungslohnController {

    private final VerrechnungslohnService verrechnungslohnService;

    @GetMapping
    public ResponseEntity<VerrechnungslohnErgebnisDto> berechne(
            @RequestParam @Min(2000) @Max(2100) int jahr) {
        return ResponseEntity.ok(verrechnungslohnService.berechne(jahr));
    }

    @PostMapping("/uebernehmen")
    public ResponseEntity<Map<String, Object>> uebernehmen(
            @Valid @RequestBody VerrechnungslohnUebernehmenRequest request) {
        int aktualisiert = verrechnungslohnService.uebernehmen(request);
        return ResponseEntity.ok(Map.of(
                "aktualisierteArbeitsgaenge", aktualisiert,
                "jahr", request.getJahr()));
    }
}
