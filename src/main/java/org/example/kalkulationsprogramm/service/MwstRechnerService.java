package org.example.kalkulationsprogramm.service;

import lombok.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reiner Rechen-Service fuer Mehrwertsteuer-Berechnungen.
 *
 * Aus zwei beliebigen der drei Werte (netto, brutto, satz) wird der dritte
 * abgeleitet. Wird sowohl im Backend (Beleg-Aufteilung, Verrechnungslohn)
 * als auch ueber den Endpoint {@code POST /api/buchhaltung/mwst-rechner}
 * vom Frontend-Rechner aufgerufen.
 *
 * Alle Ergebnisse werden auf 2 Nachkommastellen kaufmaennisch gerundet
 * (HALF_UP) — konsistent zur restlichen Beleg-Anzeige.
 */
@Service
public class MwstRechnerService {

    private static final int RUNDUNGS_NACHKOMMASTELLEN = 2;
    private static final int SATZ_NACHKOMMASTELLEN = 2;
    private static final BigDecimal HUNDERT = new BigDecimal("100");

    /** Plausible Grenzen — schuetzen vor DoS via riesigen BigDecimals und vor numerischen Edge-Cases. */
    private static final BigDecimal MAX_BETRAG = new BigDecimal("1000000000"); // 1 Mrd. EUR
    private static final BigDecimal MIN_SATZ = BigDecimal.ZERO;
    private static final BigDecimal MAX_SATZ = new BigDecimal("100");

    private void validiereEingaben(BigDecimal netto, BigDecimal brutto, BigDecimal satz) {
        if (netto != null) ausserhalb("netto", netto, MAX_BETRAG.negate(), MAX_BETRAG);
        if (brutto != null) ausserhalb("brutto", brutto, MAX_BETRAG.negate(), MAX_BETRAG);
        if (satz != null) ausserhalb("satzProzent", satz, MIN_SATZ, MAX_SATZ);
    }

    private static void ausserhalb(String feld, BigDecimal v, BigDecimal min, BigDecimal max) {
        if (v.compareTo(min) < 0 || v.compareTo(max) > 0) {
            throw new IllegalArgumentException(feld + " liegt ausserhalb des erlaubten Bereichs");
        }
    }

    /**
     * Rechnet aus zwei der drei Eingaben den fehlenden Wert plus den
     * Steuerbetrag. Genau eines der drei Felder muss null sein.
     *
     * Akzeptierte Kombinationen:
     *   netto + satz       -> brutto + mwstBetrag
     *   brutto + satz      -> netto + mwstBetrag
     *   netto + brutto     -> satz + mwstBetrag
     */
    public MwstErgebnis berechne(BigDecimal netto, BigDecimal brutto, BigDecimal satzProzent) {
        int gesetzt = (netto != null ? 1 : 0) + (brutto != null ? 1 : 0) + (satzProzent != null ? 1 : 0);
        if (gesetzt < 2) {
            throw new IllegalArgumentException("Mindestens zwei Werte (netto, brutto, satz) muessen angegeben sein");
        }
        validiereEingaben(netto, brutto, satzProzent);

        if (netto != null && satzProzent != null && brutto == null) {
            BigDecimal mwst = netto.multiply(satzProzent).divide(HUNDERT, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP);
            BigDecimal nettoR = scale2(netto);
            BigDecimal bruttoR = scale2(nettoR.add(mwst));
            return new MwstErgebnis(nettoR, bruttoR, satzProzent.setScale(SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP), mwst);
        }
        if (brutto != null && satzProzent != null && netto == null) {
            // brutto = netto * (1 + satz/100)  -> netto = brutto / (1 + satz/100)
            BigDecimal faktor = BigDecimal.ONE.add(satzProzent.divide(HUNDERT, 10, RoundingMode.HALF_UP));
            BigDecimal nettoR = brutto.divide(faktor, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP);
            BigDecimal bruttoR = scale2(brutto);
            BigDecimal mwst = scale2(bruttoR.subtract(nettoR));
            return new MwstErgebnis(nettoR, bruttoR, satzProzent.setScale(SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP), mwst);
        }
        if (netto != null && brutto != null && satzProzent == null) {
            BigDecimal nettoR = scale2(netto);
            BigDecimal bruttoR = scale2(brutto);
            BigDecimal mwst = bruttoR.subtract(nettoR);
            if (nettoR.signum() == 0) {
                if (bruttoR.signum() == 0) {
                    return new MwstErgebnis(nettoR, bruttoR, BigDecimal.ZERO.setScale(SATZ_NACHKOMMASTELLEN), mwst);
                }
                // Netto = 0 + Brutto != 0 ist mathematisch unsinnig (unendlicher Satz).
                throw new IllegalArgumentException(
                        "Aus netto=0 mit brutto!=0 laesst sich kein Steuersatz ableiten");
            }
            BigDecimal satz = mwst.multiply(HUNDERT).divide(nettoR, SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP);
            return new MwstErgebnis(nettoR, bruttoR, satz, mwst);
        }
        // Alle drei gesetzt: nur Echo + Differenz
        BigDecimal nettoR = scale2(netto);
        BigDecimal bruttoR = scale2(brutto);
        BigDecimal mwst = bruttoR.subtract(nettoR);
        BigDecimal satzR = satzProzent != null
                ? satzProzent.setScale(SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(SATZ_NACHKOMMASTELLEN);
        return new MwstErgebnis(nettoR, bruttoR, satzR, mwst);
    }

    /**
     * Zerlegt einen Brutto-Betrag in netto + mwst bei gegebenem Satz —
     * Spezialfall, der haeufig aus der Beleg-Aufteilung gebraucht wird
     * (Position hat brutto + satz, netto wird abgeleitet).
     */
    public BigDecimal nettoAusBrutto(BigDecimal brutto, BigDecimal satzProzent) {
        if (brutto == null || satzProzent == null) return null;
        BigDecimal faktor = BigDecimal.ONE.add(satzProzent.divide(HUNDERT, 10, RoundingMode.HALF_UP));
        return brutto.divide(faktor, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP);
    }

    /**
     * Brutto aus netto + Satz.
     */
    public BigDecimal bruttoAusNetto(BigDecimal netto, BigDecimal satzProzent) {
        if (netto == null || satzProzent == null) return null;
        BigDecimal mwst = netto.multiply(satzProzent).divide(HUNDERT, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP);
        return scale2(netto).add(mwst);
    }

    private static BigDecimal scale2(BigDecimal v) {
        return v.setScale(RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP);
    }

    @Value
    public static class MwstErgebnis {
        BigDecimal netto;
        BigDecimal brutto;
        BigDecimal satzProzent;
        BigDecimal mwstBetrag;
    }
}
