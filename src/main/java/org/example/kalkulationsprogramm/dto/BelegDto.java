package org.example.kalkulationsprogramm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs für das Beleg-Modul (Buchhaltung).
 */
public class BelegDto {

    private BelegDto() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String belegKategorie;
        private String status;
        private String kiAnalyseStatus;
        private LocalDate belegDatum;
        private String belegNummer;
        private String beschreibung;
        private BigDecimal betragNetto;
        private BigDecimal betragBrutto;
        private BigDecimal mwstSatz;
        private String zahlungsart;
        private Long lieferantId;
        private String lieferantName;
        private Long sachkontoId;
        private String sachkontoBezeichnung;
        private String sachkontoNummer;
        private String sachkontoTyp;
        private String kiVorgeschlagenerLieferant;
        private BigDecimal kiConfidence;
        private String kiFehlerText;
        private String originalDateiname;
        private String mimeType;
        private LocalDateTime uploadDatum;
        private Long uploadedById;
        private String uploadedByName;
        private LocalDateTime validiertAm;
        private Long validiertVonId;
        private String validiertVonName;
        private String notiz;
    }

    /**
     * Request-Body für die Validierung am PC. Alle Felder optional — nur das, was
     * der Buchhalter geändert hat, wird gesendet.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String belegKategorie;
        private String status;
        private LocalDate belegDatum;
        private String belegNummer;
        private String beschreibung;
        private BigDecimal betragNetto;
        private BigDecimal betragBrutto;
        private BigDecimal mwstSatz;
        private String zahlungsart;
        private Long lieferantId;
        private Long sachkontoId;
        private String notiz;
    }

    /**
     * Antwort von /api/buchhaltung/me/permissions — sagt dem Frontend, ob der
     * eingeloggte Mitarbeiter Belege scannen / sehen darf.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionResponse {
        private boolean darfScannen;
        private boolean darfSehen;
    }

    /**
     * Eintrag im Kassenbuch (chronologisch). Saldo ist der laufende Bestand
     * NACH dieser Bewegung.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KassenBewegung {
        private Long belegId;
        private LocalDate datum;
        private String kategorie;
        private String beschreibung;
        private String lieferantName;
        private BigDecimal betrag;          // signiert: + Einnahme, - Ausgabe/Privatentnahme
        private BigDecimal saldoNachher;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KassenbuchResponse {
        private BigDecimal saldoStart;
        private BigDecimal saldoEnde;
        private BigDecimal summeEinnahmen;
        private BigDecimal summeAusgaben;
        private BigDecimal summePrivatentnahmen;
        private List<KassenBewegung> bewegungen;
    }
}
