package org.example.kalkulationsprogramm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kalkulationsprogramm.domain.Beleg;
import org.example.kalkulationsprogramm.domain.BelegKiAnalyseStatus;
import org.example.kalkulationsprogramm.repository.BelegRepository;
import org.example.kalkulationsprogramm.repository.LieferantenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Asynchrone Belegextraktion via GeminiDokumentAnalyseService.
 *
 * Wird vom BelegService nach erfolgreichem Upload aufgerufen. Befüllt
 * Geschäftsdaten + KI-Vorschlag-Felder am Beleg. Der Mitarbeiter am Handy
 * scannt währenddessen schon den nächsten Beleg — diese Methode darf den
 * Upload-Request nicht blockieren.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BelegKiAnalyseService {

    private final BelegRepository belegRepository;
    private final LieferantenRepository lieferantenRepository;
    private final GeminiDokumentAnalyseService geminiService;
    private final ObjectMapper objectMapper;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Async
    @Transactional
    public void analysiereBelegAsync(Long belegId) {
        Beleg beleg = belegRepository.findById(belegId).orElse(null);
        if (beleg == null) {
            log.warn("KI-Analyse: Beleg {} nicht gefunden", belegId);
            return;
        }
        beleg.setKiAnalyseStatus(BelegKiAnalyseStatus.LAEUFT);
        belegRepository.save(beleg);

        try {
            Path datei = Paths.get(uploadPath, "belege", beleg.getGespeicherterDateiname());
            var ergebnis = geminiService.analyzeFile(datei, beleg.getOriginalDateiname());

            if (ergebnis == null) {
                beleg.setKiAnalyseStatus(BelegKiAnalyseStatus.FAILED);
                beleg.setKiFehlerText("KI-Analyse lieferte kein Ergebnis");
                belegRepository.save(beleg);
                return;
            }

            // Geschäftsdaten übernehmen (alle nullable — Buchhalter korrigiert am PC)
            beleg.setBelegNummer(ergebnis.getDokumentNummer());
            beleg.setBelegDatum(ergebnis.getDokumentDatum());
            beleg.setBetragNetto(ergebnis.getBetragNetto());
            beleg.setBetragBrutto(ergebnis.getBetragBrutto());
            beleg.setMwstSatz(ergebnis.getMwstSatz());
            beleg.setZahlungsart(ergebnis.getZahlungsart());
            if (ergebnis.getAiConfidence() != null) {
                beleg.setKiConfidence(java.math.BigDecimal.valueOf(ergebnis.getAiConfidence())
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
            beleg.setKiVorgeschlagenerLieferant(ergebnis.getLieferantName());

            // Versuche Lieferant per Namen zu matchen (nur exakt / case-insensitive)
            if (ergebnis.getLieferantName() != null && !ergebnis.getLieferantName().isBlank()
                    && beleg.getLieferant() == null) {
                String gesucht = ergebnis.getLieferantName().toLowerCase(Locale.ROOT);
                lieferantenRepository.findAll().stream()
                        .filter(l -> !Boolean.FALSE.equals(l.getIstAktiv()))
                        .filter(l -> l.getLieferantenname() != null
                                && l.getLieferantenname().toLowerCase(Locale.ROOT).equals(gesucht))
                        .findFirst()
                        .ifPresent(beleg::setLieferant);
            }

            // Komplettes Extraktions-JSON für Debug / spätere Re-Analyse
            try {
                beleg.setKiExtraktionJson(objectMapper.writeValueAsString(ergebnis));
            } catch (Exception ignored) {
                // Nicht kritisch — Hauptfelder sind bereits gesetzt
            }

            beleg.setKiAnalyseStatus(BelegKiAnalyseStatus.DONE);
            beleg.setKiFehlerText(null);
            belegRepository.save(beleg);

        } catch (Exception e) {
            log.error("KI-Analyse fuer Beleg {} fehlgeschlagen: {}", belegId, e.getMessage(), e);
            beleg.setKiAnalyseStatus(BelegKiAnalyseStatus.FAILED);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (msg.length() > 1000) {
                msg = msg.substring(0, 1000);
            }
            beleg.setKiFehlerText(msg);
            belegRepository.save(beleg);
        }
    }
}
