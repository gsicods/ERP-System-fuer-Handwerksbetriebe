package org.example.kalkulationsprogramm.service;

import java.math.BigDecimal;

/**
 * Fachliche Ausnahme: Eine Buchung wuerde den Bar-Saldo unter den
 * konfigurierten Mindestbestand fallen lassen. Wird vom BelegController als
 * HTTP 409 Conflict an den Client zurueckgegeben — der Frontend-Editor zeigt
 * dem User dann den vorgeschlagenen Workflow (vorher Bank-Abhebung oder
 * Privateinlage buchen).
 */
public class KasseUnterdeckungException extends RuntimeException {

    private final BigDecimal projizierterSaldo;
    private final BigDecimal mindestbestand;

    public KasseUnterdeckungException(BigDecimal projizierterSaldo, BigDecimal mindestbestand) {
        super("Kasse wuerde auf " + projizierterSaldo + " EUR rutschen "
                + "(Mindestbestand: " + mindestbestand + " EUR). "
                + "Bitte vorher Bank-Abhebung oder Privateinlage buchen.");
        this.projizierterSaldo = projizierterSaldo;
        this.mindestbestand = mindestbestand;
    }

    public BigDecimal getProjizierterSaldo() {
        return projizierterSaldo;
    }

    public BigDecimal getMindestbestand() {
        return mindestbestand;
    }
}
