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
        private String dokumentTyp;          // KI-Klassifikation: RECHNUNG, GUTSCHRIFT, ...
        private Boolean istUmbuchung;        // belegfreie Buchung (Privatentnahme, Kasse->Bank etc.)
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
        // Echte Zuordnung Kostenstelle ("wofuer war die Ausgabe")
        private Long kostenstelleId;
        private String kostenstelleBezeichnung;
        private String kostenstelleTyp;
        private Boolean kostenstelleIstFixkosten;
        private String kiVorgeschlagenerLieferant;
        private BigDecimal kiConfidence;
        // KI-Agent-Vorschlag fuer Kostenstelle + Sachkonto (aus DB-Liste gewaehlt)
        private Long kiVorgeschlagenerKostenstelleId;
        private String kiVorgeschlagenerKostenstelleBezeichnung;
        private Long kiVorgeschlagenerSachkontoId;
        private String kiVorgeschlagenerSachkontoBezeichnung;
        private BigDecimal kiKostenkontoConfidence;
        private String kiKostenkontoBegruendung;
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
        // Falls aus dem Beleg automatisch ein Eingangsrechnungs-Datensatz erzeugt wurde,
        // verweist dieses Feld auf die Eingangsrechnungs-ID (LieferantGeschaeftsdokument.id).
        private Long eingangsrechnungId;
        // Beleg-Aufteilung (VOLLSTAENDIG / TEILWEISE) und die per Checkbox-Auswahl
        // berechneten Firma-Summen. Bei VOLLSTAENDIG sind die betragFirma*-Felder null
        // und der Buchhalter liest die Standard-Betraege.
        private String aufteilungsModus;
        private BigDecimal betragFirmaNetto;
        private BigDecimal betragFirmaBrutto;
        private BigDecimal betragFirmaMwst;
        private List<PositionResponse> positionen;
    }

    /**
     * Eine einzelne KI-extrahierte Beleg-Position fuer das Checkbox-UI am Handy.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionResponse {
        private Long id;
        private int sortierung;
        private String beschreibung;
        private BigDecimal menge;
        private String einheit;
        private BigDecimal einzelpreis;
        private BigDecimal betragNetto;
        private BigDecimal betragBrutto;
        private BigDecimal mwstSatz;
        private boolean istFuerFirma;
    }

    /**
     * Request des Mobile-Clients zum Speichern der Checkbox-Auswahl.
     * {@code firmaPositionIds} ist die vollstaendige Ist-Liste — alle nicht
     * enthaltenen Positionen werden auf {@code istFuerFirma=false} gesetzt.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionAuswahlRequest {
        private List<Long> firmaPositionIds;
    }

    /**
     * Request fuer den MwSt-Rechner ({@code POST /api/buchhaltung/mwst-rechner}).
     * Genau eines der drei Felder darf null sein — der Service rechnet es aus.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MwstRechnerRequest {
        private BigDecimal netto;
        private BigDecimal brutto;
        private BigDecimal satzProzent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MwstRechnerResponse {
        private BigDecimal netto;
        private BigDecimal brutto;
        private BigDecimal satzProzent;
        private BigDecimal mwstBetrag;
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
        private Long kostenstelleId;
        private String notiz;
        // Wechsel zwischen VOLLSTAENDIG <-> TEILWEISE am PC moeglich, falls der
        // Buchhalter nachtraeglich umschwenkt (z.B. urspruenglich VOLLSTAENDIG
        // gescannt, jetzt doch nur Teile fuer Firma).
        private String aufteilungsModus;
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
        private BigDecimal summePrivateinlagen;
        private List<KassenBewegung> bewegungen;
    }

    /**
     * Request zur Erfassung einer Umbuchung OHNE Beleg-Datei.
     *
     * Use-Cases:
     *  - Privatentnahme (Bargeld aus Kasse, kein Beleg vorhanden)
     *  - Umbuchung Kasse -> Bank
     *  - Privat -> Firma
     *  - Geldeingang auf Konto (ohne Bankauszug-Scan)
     *
     * Pflichtfelder: belegKategorie + betragBrutto + belegDatum.
     * Die Kategorie muss eine Kassen-Bewegungskategorie sein
     * (KASSE_EINNAHME|KASSE_AUSGABE|PRIVATENTNAHME|PRIVATEINLAGE|BANK|KREDITKARTE) — eine
     * Eingangsrechnung kann nicht als Umbuchung erfasst werden, weil das
     * weder GoBD- noch DSGVO-konform waere (es waere dann eine Buchung ohne
     * Originalbeleg, die als Rechnung in die Buchhaltung einfliesst).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UmbuchungCreateRequest {
        private String belegKategorie;
        private LocalDate belegDatum;
        private BigDecimal betragBrutto;
        private String beschreibung;
        private String zahlungsart;
        private Long sachkontoId;
        private String notiz;
    }
}
