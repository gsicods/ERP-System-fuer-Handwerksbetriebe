package org.example.kalkulationsprogramm.service;

import org.example.kalkulationsprogramm.domain.Beleg;
import org.example.kalkulationsprogramm.domain.BelegKategorie;
import org.example.kalkulationsprogramm.domain.BelegStatus;
import org.example.kalkulationsprogramm.domain.KasseEinstellung;
import org.example.kalkulationsprogramm.repository.BelegRepository;
import org.example.kalkulationsprogramm.repository.KasseEinstellungRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/**
 * Unit-Tests fuer den KasseSaldoService.
 *
 * Saldo = + KASSE_EINNAHME + PRIVATEINLAGE
 *         - KASSE_AUSGABE  - PRIVATENTNAHME
 *
 * Nur Belege im Status VALIDIERT zaehlen — NEU/VERWORFEN bleiben aussen vor.
 */
@ExtendWith(MockitoExtension.class)
class KasseSaldoServiceTest {

    @Mock private BelegRepository belegRepository;
    @Mock private KasseEinstellungRepository kasseEinstellungRepository;

    @InjectMocks
    private KasseSaldoService service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient()
                .when(kasseEinstellungRepository.findSingleton()).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("berechneAktuellenSaldo summiert Einnahmen + Einlagen minus Ausgaben + Entnahmen")
    void berechneAktuellenSaldo_summiertKorrekt() {
        given(belegRepository.findValidierteByKategorien(any(BelegStatus.class), anyList()))
                .willReturn(List.of(
                        bar(BelegKategorie.KASSE_EINNAHME, "500.00"),
                        bar(BelegKategorie.PRIVATEINLAGE, "200.00"),
                        bar(BelegKategorie.KASSE_AUSGABE, "150.00"),
                        bar(BelegKategorie.PRIVATENTNAHME, "100.00")));

        BigDecimal saldo = service.berechneAktuellenSaldo();

        // 500 + 200 - 150 - 100 = 450
        assertThat(saldo).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("berechneAktuellenSaldo: leere Bewegungsliste -> 0")
    void berechneAktuellenSaldo_leer_nullEuro() {
        given(belegRepository.findValidierteByKategorien(any(BelegStatus.class), anyList()))
                .willReturn(List.of());

        assertThat(service.berechneAktuellenSaldo()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("projiziereSaldo zieht alten Beitrag ab und addiert neuen")
    void projiziereSaldo_alteAusgabeWirdZuruckgenommen_neueEinnahmeAddiert() {
        // Aktueller Saldo aus Repo: nur die eine Einnahme = +100.
        given(belegRepository.findValidierteByKategorien(any(BelegStatus.class), anyList()))
                .willReturn(List.of(bar(BelegKategorie.KASSE_EINNAHME, "100.00")));

        // Update-Simulation: alter Eintrag war Ausgabe 30 (Beitrag -30),
        // neuer Eintrag ist Einnahme 50 (Beitrag +50).
        // Erwartet: 100 - (-30) + (+50) = 100 + 30 + 50 = 180.
        BigDecimal projiziert = service.projiziereSaldo(
                BelegKategorie.KASSE_AUSGABE, new BigDecimal("30.00"),
                BelegKategorie.KASSE_EINNAHME, new BigDecimal("50.00"));

        assertThat(projiziert).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("projiziereSaldo: Neuanlage (kein alter Beitrag) addiert nur den neuen")
    void projiziereSaldo_neuanlage_addiertNurNeu() {
        given(belegRepository.findValidierteByKategorien(any(BelegStatus.class), anyList()))
                .willReturn(List.of(bar(BelegKategorie.KASSE_EINNAHME, "200.00")));

        BigDecimal projiziert = service.projiziereSaldo(
                null, null,
                BelegKategorie.KASSE_AUSGABE, new BigDecimal("70.00"));

        // 200 - 70 = 130
        assertThat(projiziert).isEqualByComparingTo("130.00");
    }

    @Test
    @DisplayName("getMindestbestand: ohne Konfiguration -> 0")
    void getMindestbestand_ohneEinstellung_nullEuro() {
        assertThat(service.getMindestbestand()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("getMindestbestand: liefert konfigurierten Wert")
    void getMindestbestand_mitEinstellung_liefertWert() {
        KasseEinstellung k = new KasseEinstellung();
        k.setMindestbestand(new BigDecimal("250.00"));
        given(kasseEinstellungRepository.findSingleton()).willReturn(Optional.of(k));

        assertThat(service.getMindestbestand()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("assertSaldoMindestensMindestbestand: unter Mindestbestand -> KasseUnterdeckungException")
    void assertSaldo_unterMindestbestand_wirftException() {
        KasseEinstellung k = new KasseEinstellung();
        k.setMindestbestand(new BigDecimal("100.00"));
        given(kasseEinstellungRepository.findSingleton()).willReturn(Optional.of(k));

        assertThatThrownBy(() -> service.assertSaldoMindestensMindestbestand(new BigDecimal("99.99")))
                .isInstanceOf(KasseUnterdeckungException.class)
                .matches(e -> ((KasseUnterdeckungException) e).getMindestbestand()
                        .compareTo(new BigDecimal("100.00")) == 0)
                .matches(e -> ((KasseUnterdeckungException) e).getProjizierterSaldo()
                        .compareTo(new BigDecimal("99.99")) == 0);
    }

    @Test
    @DisplayName("assertSaldoMindestensMindestbestand: am Mindestbestand -> erlaubt")
    void assertSaldo_genauMindestbestand_keinFehler() {
        KasseEinstellung k = new KasseEinstellung();
        k.setMindestbestand(new BigDecimal("100.00"));
        given(kasseEinstellungRepository.findSingleton()).willReturn(Optional.of(k));

        // soll nicht werfen
        service.assertSaldoMindestensMindestbestand(new BigDecimal("100.00"));
    }

    private static Beleg bar(BelegKategorie kategorie, String brutto) {
        Beleg b = new Beleg();
        b.setBelegKategorie(kategorie);
        b.setBetragBrutto(new BigDecimal(brutto));
        b.setStatus(BelegStatus.VALIDIERT);
        return b;
    }
}
