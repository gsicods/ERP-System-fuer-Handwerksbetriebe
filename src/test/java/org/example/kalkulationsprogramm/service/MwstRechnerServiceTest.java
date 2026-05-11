package org.example.kalkulationsprogramm.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MwstRechnerServiceTest {

    private final MwstRechnerService rechner = new MwstRechnerService();

    @Test
    void nettoUndSatzLiefertBrutto() {
        // 100 EUR + 19% = 119 EUR
        MwstRechnerService.MwstErgebnis e = rechner.berechne(
                new BigDecimal("100.00"), null, new BigDecimal("19"));

        assertThat(e.getNetto()).isEqualByComparingTo("100.00");
        assertThat(e.getMwstBetrag()).isEqualByComparingTo("19.00");
        assertThat(e.getBrutto()).isEqualByComparingTo("119.00");
    }

    @Test
    void bruttoUndSatzLiefertNetto() {
        // 119 EUR / 1.19 = 100 EUR
        MwstRechnerService.MwstErgebnis e = rechner.berechne(
                null, new BigDecimal("119.00"), new BigDecimal("19"));

        assertThat(e.getNetto()).isEqualByComparingTo("100.00");
        assertThat(e.getBrutto()).isEqualByComparingTo("119.00");
        assertThat(e.getMwstBetrag()).isEqualByComparingTo("19.00");
    }

    @Test
    void nettoUndBruttoLeitetSatzAb() {
        // 100 -> 107 == 7%
        MwstRechnerService.MwstErgebnis e = rechner.berechne(
                new BigDecimal("100"), new BigDecimal("107"), null);

        assertThat(e.getSatzProzent()).isEqualByComparingTo("7.00");
        assertThat(e.getMwstBetrag()).isEqualByComparingTo("7.00");
    }

    @Test
    void fehltZweiterWertWirftException() {
        assertThatThrownBy(() -> rechner.berechne(new BigDecimal("100"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nettoAusBruttoMitSiebenProzent() {
        // 10.70 Brutto bei 7% MwSt = 10.00 Netto
        BigDecimal netto = rechner.nettoAusBrutto(new BigDecimal("10.70"), new BigDecimal("7"));

        assertThat(netto).isEqualByComparingTo("10.00");
    }

    @Test
    void bruttoAusNettoMitNeunzehnProzent() {
        BigDecimal brutto = rechner.bruttoAusNetto(new BigDecimal("50.00"), new BigDecimal("19"));

        assertThat(brutto).isEqualByComparingTo("59.50");
    }

    @Test
    void nettoUndBruttoNullErgibtSatzNull() {
        // Netto 0 + Brutto 0 -> Satz 0 (kein Eingabefehler, einfach leerer Beleg)
        MwstRechnerService.MwstErgebnis e = rechner.berechne(
                BigDecimal.ZERO, BigDecimal.ZERO, null);

        assertThat(e.getSatzProzent()).isEqualByComparingTo("0.00");
        assertThat(e.getMwstBetrag()).isEqualByComparingTo("0.00");
    }

    @Test
    void nettoNullMitBruttoUngleichNullWirftException() {
        // netto=0 + brutto>0 -> unsinnig (Division durch 0 / unendlicher Satz)
        assertThatThrownBy(() -> rechner.berechne(BigDecimal.ZERO, new BigDecimal("10"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativerSatzWirftException() {
        assertThatThrownBy(() -> rechner.berechne(new BigDecimal("100"), null, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void satzUeber100WirftException() {
        assertThatThrownBy(() -> rechner.berechne(new BigDecimal("100"), null, new BigDecimal("150")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void riesigerBetragWirftException() {
        BigDecimal absurd = new BigDecimal("1e20");
        assertThatThrownBy(() -> rechner.berechne(absurd, null, new BigDecimal("19")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
