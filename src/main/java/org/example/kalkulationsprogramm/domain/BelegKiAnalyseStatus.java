package org.example.kalkulationsprogramm.domain;

/**
 * Status der asynchronen KI-Analyse eines Belegs.
 * Beim Mobile-Upload wird der Beleg mit PENDING angelegt; ein @Async-Job
 * extrahiert dann die Daten via Gemini und setzt DONE oder FAILED.
 */
public enum BelegKiAnalyseStatus {
    PENDING,
    LAEUFT,
    DONE,
    FAILED
}
