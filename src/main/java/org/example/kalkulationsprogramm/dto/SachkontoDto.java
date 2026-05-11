package org.example.kalkulationsprogramm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class SachkontoDto {

    private SachkontoDto() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nummer;
        private String bezeichnung;
        private String kontoTyp;
        private String beschreibung;
        private boolean aktiv;
        private int sortierung;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpsertRequest {
        private String nummer;
        private String bezeichnung;
        private String kontoTyp;
        private String beschreibung;
        private Boolean aktiv;
        private Integer sortierung;
    }

    /**
     * Eine Zeile in der Konten-Auswertung: Summe brutto pro Sachkonto im
     * gewählten Zeitraum. NULL-Konto (kein Sachkonto zugewiesen) wird als
     * eigene Zeile geliefert, damit der Buchhalter sieht, was noch zugeordnet
     * werden muss.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuswertungZeile {
        private Long sachkontoId;
        private String nummer;
        private String bezeichnung;
        private String kontoTyp;
        private BigDecimal summe;
        private int anzahlBelege;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuswertungResponse {
        private String von;
        private String bis;
        private BigDecimal summeAufwand;
        private BigDecimal summeErtrag;
        private BigDecimal summePrivat;
        private BigDecimal summeOhneKonto;
        private List<AuswertungZeile> zeilen;
    }
}
