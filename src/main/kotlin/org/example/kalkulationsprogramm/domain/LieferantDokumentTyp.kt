package org.example.kalkulationsprogramm.domain;

/**
 * Dokumenttypen für Lieferanten-Dokumente.
 * Bilden die Dokumentenkette: Anfrage → Auftragsbestätigung → Lieferschein →
 * Rechnung
 * SONSTIG für nicht-geschäftliche Dokumente (Kataloge, Infoblätter etc.)
 * BELEG steuert die Berechtigung für das Buchhaltungs-Beleg-Modul
 * (mobile Scanner + PC-Validierung) über das gleiche Abteilungs-Permission-System.
 */
enum class LieferantDokumentTyp {
    ANGEBOT,
    AUFTRAGSBESTAETIGUNG,
    LIEFERSCHEIN,
    RECHNUNG,
    GUTSCHRIFT, // Gutschriften vom Lieferanten
    SONSTIG, // Nicht-Geschäftsdokumente (Katalog, Info etc.)
    BELEG // Buchhaltungs-Belege (Kasse, Privatentnahme, Bank, Einmalbelege)
}
