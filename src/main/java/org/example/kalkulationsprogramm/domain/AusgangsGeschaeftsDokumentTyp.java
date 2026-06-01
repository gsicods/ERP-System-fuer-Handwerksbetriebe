package org.example.kalkulationsprogramm.domain;

/**
 * Typen für ausgehende Geschäftsdokumente.
 *
 * <p>Die Mahn-Typen ({@link #ZAHLUNGSERINNERUNG}, {@link #ERSTE_MAHNUNG},
 * {@link #ZWEITE_MAHNUNG}) sind <strong>virtuell</strong>: Mahnungen werden
 * weiterhin als {@code ProjektGeschaeftsdokument} persistiert. Die Werte
 * existieren nur, damit die Mahn-Hierarchie (Rechnung → Zahlungserinnerung →
 * 1. Mahnung → 2. Mahnung) im Ausgangs-Dokumente-Tab des Projekt-Editors
 * sichtbar gemacht werden kann.</p>
 */
public enum AusgangsGeschaeftsDokumentTyp {
    ANGEBOT,
    /**
     * Nachtragsangebot: ein zusätzliches Basisdokument (eigener Wurzel-Vorgang)
     * neben dem ursprünglichen {@link #ANGEBOT}. Verhält sich wie ein Angebot —
     * eigener Nummernkreis (Präfix "NA"), eigene Folgedokumente (AB, Rechnungen)
     * und digitale Freigabe. Setzt voraus, dass für Projekt/Anfrage bereits ein
     * Angebot existiert (siehe {@code AusgangsGeschaeftsDokumentService.erstellen}).
     */
    NACHTRAGSANGEBOT,
    AUFTRAGSBESTAETIGUNG,
    RECHNUNG,
    TEILRECHNUNG,
    ABSCHLAGSRECHNUNG,
    SCHLUSSRECHNUNG,
    GUTSCHRIFT,
    STORNO,
    ZAHLUNGSERINNERUNG,
    ERSTE_MAHNUNG,
    ZWEITE_MAHNUNG
}
