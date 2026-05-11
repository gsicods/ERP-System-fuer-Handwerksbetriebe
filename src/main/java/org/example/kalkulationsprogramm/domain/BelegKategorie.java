package org.example.kalkulationsprogramm.domain;

/**
 * Buchhalterische Kategorie eines Belegs. Vom Buchhalter beim Validieren am PC
 * gesetzt. UNZUGEORDNET ist der Default direkt nach dem Scan.
 *
 * KASSE_EINNAHME / KASSE_AUSGABE / PRIVATENTNAHME / PRIVATEINLAGE zählen in das
 * Kassenbuch (laufender Bar-Saldo). BANK / KREDITKARTE / SONSTIGER_BELEG zählen
 * nicht ins Bar-Kassenbuch.
 *
 * PRIVATENTNAHME = Inhaber nimmt Geld aus der Firma ins Private (Ausgang).
 * PRIVATEINLAGE  = Inhaber legt Geld aus dem Privaten in die Firma (Eingang).
 */
public enum BelegKategorie {
    UNZUGEORDNET,
    KASSE_EINNAHME,
    KASSE_AUSGABE,
    PRIVATENTNAHME,
    PRIVATEINLAGE,
    BANK,
    KREDITKARTE,
    SONSTIGER_BELEG;

    public boolean istKassenBewegung() {
        return this == KASSE_EINNAHME || this == KASSE_AUSGABE
                || this == PRIVATENTNAHME || this == PRIVATEINLAGE;
    }

    public boolean istAusgang() {
        return this == KASSE_AUSGABE || this == PRIVATENTNAHME;
    }
}
