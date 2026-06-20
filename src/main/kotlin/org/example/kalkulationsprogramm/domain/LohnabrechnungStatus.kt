package org.example.kalkulationsprogramm.domain;

/**
 * Status einer Lohnabrechnung im Importprozess.
 */
enum class LohnabrechnungStatus {
    /**
     * Lohnabrechnung wurde importiert, aber noch nicht von KI analysiert.
     */
    IMPORTIERT,

    /**
     * KI-Analyse läuft.
     */
    WIRD_ANALYSIERT,

    /**
     * KI-Analyse abgeschlossen, Daten extrahiert.
     */
    ANALYSIERT,

    /**
     * Fehler bei der Analyse.
     */
    FEHLER
}
