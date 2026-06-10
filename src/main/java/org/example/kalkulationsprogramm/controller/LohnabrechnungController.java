package org.example.kalkulationsprogramm.controller;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.dto.LohnabrechnungDto;
import org.example.kalkulationsprogramm.service.LohnabrechnungService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller für Lohnabrechnungen.
 */
@RestController
@RequestMapping("/api/lohnabrechnungen")
@RequiredArgsConstructor
public class LohnabrechnungController {

    private final LohnabrechnungService lohnabrechnungService;

    /**
     * Findet alle Lohnabrechnungen eines Mitarbeiters.
     */
    @GetMapping("/mitarbeiter/{mitarbeiterId}")
    public ResponseEntity<List<LohnabrechnungDto>> getByMitarbeiter(@PathVariable Long mitarbeiterId) {
        return ResponseEntity.ok(lohnabrechnungService.findByMitarbeiterId(mitarbeiterId));
    }

    /**
     * Findet alle Lohnabrechnungen eines Jahres.
     */
    @GetMapping("/jahr/{jahr}")
    public ResponseEntity<List<LohnabrechnungDto>> getByJahr(@PathVariable Integer jahr) {
        return ResponseEntity.ok(lohnabrechnungService.findByJahr(jahr));
    }

    /**
     * Findet alle Lohnabrechnungen eines Steuerberaters in einem Jahr.
     */
    @GetMapping("/steuerberater/{steuerberaterId}/jahr/{jahr}")
    public ResponseEntity<List<LohnabrechnungDto>> getBySteuerberaterAndJahr(
            @PathVariable Long steuerberaterId, @PathVariable Integer jahr) {
        return ResponseEntity.ok(lohnabrechnungService.findBySteuerberaterAndJahr(steuerberaterId, jahr));
    }

    /**
     * Findet alle verfügbaren Jahre.
     */
    @GetMapping("/jahre")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        return ResponseEntity.ok(lohnabrechnungService.findAvailableYears());
    }

    /**
     * Findet eine Lohnabrechnung nach ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LohnabrechnungDto> getById(@PathVariable Long id) {
        LohnabrechnungDto dto = lohnabrechnungService.findById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * Lädt die PDF einer Lohnabrechnung herunter.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        try {
            return lohnabrechnungService.findPdf(id)
                    .map(pdf -> {
                        try {
                            Resource resource = new UrlResource(pdf.pfad().toUri());
                            return ResponseEntity.ok()
                                    .contentType(MediaType.APPLICATION_PDF)
                                    .header(HttpHeaders.CONTENT_DISPOSITION,
                                            "inline; filename=\"" + pdf.anzeigeName() + "\"")
                                    .body(resource);
                        } catch (Exception e) {
                            return ResponseEntity.internalServerError().<Resource>build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Löscht eine Lohnabrechnung.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lohnabrechnungService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
