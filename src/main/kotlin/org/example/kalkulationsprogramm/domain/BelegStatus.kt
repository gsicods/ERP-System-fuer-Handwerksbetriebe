package org.example.kalkulationsprogramm.domain;

/**
 * Lifecycle eines Belegs.
 *
 * NEU: Frisch vom Mobile-Scanner oder PC-Upload, noch nicht geprüft.
 * VALIDIERT: Vom Buchhalter am PC geprüft, KI-Vorschläge korrigiert,
 *            Kategorie gesetzt. Zählt jetzt buchhalterisch.
 * VERWORFEN: Beleg ist Schrott/Duplikat — wird ausgeblendet, Datei bleibt.
 */
enum class BelegStatus {
    NEU,
    VALIDIERT,
    VERWORFEN
}
