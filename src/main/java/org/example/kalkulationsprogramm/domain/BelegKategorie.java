package org.example.kalkulationsprogramm.domain;

/**
 * Buchhalterische Kategorie eines Belegs. Vom Buchhalter beim Validieren am PC
 * gesetzt. UNZUGEORDNET ist der Default direkt nach dem Scan.
 *
 * KASSE_EINNAHME / KASSE_AUSGABE / PRIVATENTNAHME zählen in das Kassenbuch
 * (laufender Bar-Saldo). BANK / KREDITKARTE / SONSTIGER_BELEG zählen nicht ins
 * Bar-Kassenbuch.
 */
public enum BelegKategorie {
    UNZUGEORDNET,
    KASSE_EINNAHME,
    KASSE_AUSGABE,
    PRIVATENTNAHME,
    BANK,
    KREDITKARTE,
    SONSTIGER_BELEG;

    public boolean istKassenBewegung() {
        return this == KASSE_EINNAHME || this == KASSE_AUSGABE || this == PRIVATENTNAHME;
    }

    public boolean istAusgang() {
        return this == KASSE_AUSGABE || this == PRIVATENTNAHME;
    }
}
