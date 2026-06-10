package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kalkulationsprogramm.domain.Lohnabrechnung;
import org.example.kalkulationsprogramm.dto.LohnabrechnungDto;
import org.example.kalkulationsprogramm.repository.LohnabrechnungRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service für Lohnabrechnungs-Verwaltung.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LohnabrechnungService {

    private final LohnabrechnungRepository lohnabrechnungRepository;

    @Value("${file.lohnabrechnung-dir:uploads/lohnabrechnungen}")
    private String lohnabrechnungDir;

    @Value("${file.mail-attachment-dir}")
    private String mailAttachmentDir;

    /**
     * Findet alle Lohnabrechnungen eines Mitarbeiters.
     */
    @Transactional(readOnly = true)
    public List<LohnabrechnungDto> findByMitarbeiterId(Long mitarbeiterId) {
        return lohnabrechnungRepository.findByMitarbeiterIdOrderByJahrDescMonatDesc(mitarbeiterId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Findet alle Lohnabrechnungen eines Jahres.
     */
    @Transactional(readOnly = true)
    public List<LohnabrechnungDto> findByJahr(Integer jahr) {
        return lohnabrechnungRepository.findByJahrOrderByMonatDescMitarbeiterNachnameAsc(jahr)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Findet alle Lohnabrechnungen eines Steuerberaters in einem Jahr.
     */
    @Transactional(readOnly = true)
    public List<LohnabrechnungDto> findBySteuerberaterAndJahr(Long steuerberaterId, Integer jahr) {
        return lohnabrechnungRepository
                .findBySteuerberaterIdAndJahrOrderByMonatDescMitarbeiterNachnameAsc(steuerberaterId, jahr)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Findet alle verfügbaren Jahre.
     */
    @Transactional(readOnly = true)
    public List<Integer> findAvailableYears() {
        return lohnabrechnungRepository.findDistinctJahre();
    }

    /**
     * Findet eine Lohnabrechnung nach ID.
     */
    @Transactional(readOnly = true)
    public LohnabrechnungDto findById(Long id) {
        return lohnabrechnungRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Speichert eine Lohnabrechnung.
     */
    @Transactional
    public Lohnabrechnung save(Lohnabrechnung lohnabrechnung) {
        return lohnabrechnungRepository.save(lohnabrechnung);
    }

    /**
     * PDF-Datei einer Lohnabrechnung mit Anzeigename für den Download.
     */
    public record PdfDatei(Path pfad, String anzeigeName) {}

    /**
     * Löst die PDF-Datei einer Lohnabrechnung auf.
     * Gesplittete Sammel-PDFs liegen im Lohnabrechnungs-Verzeichnis,
     * ältere Importe referenzieren noch direkt den E-Mail-Anhang.
     */
    @Transactional(readOnly = true)
    public Optional<PdfDatei> findPdf(Long id) {
        return lohnabrechnungRepository.findById(id).flatMap(la -> {
            String gespeichert = la.getGespeicherterDateiname();
            if (gespeichert == null || gespeichert.isBlank()) {
                return Optional.empty();
            }

            String anzeigeName = la.getOriginalDateiname() != null
                    ? la.getOriginalDateiname()
                    : "lohnabrechnung.pdf";

            for (String dir : List.of(lohnabrechnungDir, mailAttachmentDir)) {
                Path basis = Path.of(dir).toAbsolutePath().normalize();
                Path datei = basis.resolve(gespeichert).normalize();
                // Schutz gegen Path-Traversal über manipulierte Dateinamen
                if (!datei.startsWith(basis)) {
                    log.warn("[Lohnabrechnung] Ungültiger Dateipfad für ID {}: {}", id, gespeichert);
                    return Optional.empty();
                }
                if (Files.exists(datei)) {
                    return Optional.of(new PdfDatei(datei, anzeigeName));
                }
            }

            log.warn("[Lohnabrechnung] PDF-Datei nicht gefunden für ID {} ({})", id, gespeichert);
            return Optional.empty();
        });
    }

    /**
     * Löscht eine Lohnabrechnung.
     */
    @Transactional
    public void delete(Long id) {
        lohnabrechnungRepository.deleteById(id);
    }

    /**
     * Konvertiert Entity zu DTO.
     */
    private LohnabrechnungDto toDto(Lohnabrechnung la) {
        LohnabrechnungDto dto = new LohnabrechnungDto();
        dto.setId(la.getId());
        dto.setMitarbeiterId(la.getMitarbeiter().getId());
        dto.setMitarbeiterName(la.getMitarbeiter().getVorname() + " " + la.getMitarbeiter().getNachname());
        
        if (la.getSteuerberater() != null) {
            dto.setSteuerberaterId(la.getSteuerberater().getId());
            dto.setSteuerberaterName(la.getSteuerberater().getName());
        }
        
        dto.setJahr(la.getJahr());
        dto.setMonat(la.getMonat());
        dto.setOriginalDateiname(la.getOriginalDateiname());
        dto.setDownloadUrl("/api/lohnabrechnungen/" + la.getId() + "/download");
        dto.setBruttolohn(la.getBruttolohn());
        dto.setNettolohn(la.getNettolohn());
        dto.setImportDatum(la.getImportDatum());
        dto.setStatus(la.getStatus().name());
        
        return dto;
    }
}
