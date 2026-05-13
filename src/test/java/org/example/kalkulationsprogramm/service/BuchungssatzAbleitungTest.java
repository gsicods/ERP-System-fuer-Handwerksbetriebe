package org.example.kalkulationsprogramm.service;

import org.example.kalkulationsprogramm.domain.Beleg;
import org.example.kalkulationsprogramm.domain.BelegKategorie;
import org.example.kalkulationsprogramm.domain.Sachkonto;
import org.example.kalkulationsprogramm.domain.SachkontoTyp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests fuer das Doppik-Variante-A-Mapping (Issue #61): pruft alle
 * Kombinationen von Beleg-Kategorie x Sachkonto-Typ inkl. Robustheit bei
 * fehlendem Sachkonto.
 */
class BuchungssatzAbleitungTest {

    @Test
    @DisplayName("KASSE_EINNAHME + Ertrag-Konto: Soll=Kasse, Haben=Sachkonto")
    void einnahmeMitErtragKonto() {
        Beleg b = beleg(BelegKategorie.KASSE_EINNAHME,
                sachkonto("4400", "Erlöse 19%", SachkontoTyp.ERTRAG));

        BuchungssatzAbleitung.Buchungssatz bs = BuchungssatzAbleitung.ableiten(b);

        assertThat(bs.soll()).isEqualTo(BuchungssatzAbleitung.KASSE);
        assertThat(bs.haben()).contains("Erlöse");
    }

    @Test
    @DisplayName("KASSE_EINNAHME ohne Sachkonto -> Bank-Konto als Haben (Bank->Kasse Abhebung)")
    void einnahmeOhneSachkonto_HabenIstBank() {
        Beleg b = beleg(BelegKategorie.KASSE_EINNAHME, null);

        BuchungssatzAbleitung.Buchungssatz bs = BuchungssatzAbleitung.ableiten(b);

        assertThat(bs.soll()).isEqualTo(BuchungssatzAbleitung.KASSE);
        assertThat(bs.haben()).isEqualTo(BuchungssatzAbleitung.BANK);
    }

    @Test
    @DisplayName("KASSE_AUSGABE + Aufwand-Konto: Soll=Sachkonto, Haben=Kasse")
    void ausgabeMitAufwand() {
        Beleg b = beleg(BelegKategorie.KASSE_AUSGABE,
                sachkonto("4530", "Tankkosten", SachkontoTyp.AUFWAND));

        BuchungssatzAbleitung.Buchungssatz bs = BuchungssatzAbleitung.ableiten(b);

        assertThat(bs.soll()).contains("Tankkosten");
        assertThat(bs.haben()).isEqualTo(BuchungssatzAbleitung.KASSE);
    }

    @Test
    @DisplayName("KASSE_AUSGABE ohne Sachkonto -> Soll=?")
    void ausgabeOhneSachkonto_SollIstFragezeichen() {
        Beleg b = beleg(BelegKategorie.KASSE_AUSGABE, null);

        BuchungssatzAbleitung.Buchungssatz bs = BuchungssatzAbleitung.ableiten(b);

        assertThat(bs.soll()).isEqualTo(BuchungssatzAbleitung.UNKLAR);
        assertThat(bs.haben()).isEqualTo(BuchungssatzAbleitung.KASSE);
    }

    @Test
    @DisplayName("PRIVATEINLAGE: Soll=Kasse, Haben=Privateinlage")
    void privateinlageOhneSachkonto() {
        Beleg b = beleg(BelegKategorie.PRIVATEINLAGE, null);

        BuchungssatzAbleitung.Buchungssatz bs = BuchungssatzAbleitung.ableiten(b);

        assertThat(bs.soll()).isEqualTo(BuchungssatzAbleitung.KASSE);
        assertThat(bs.haben()).isEqualTo(BuchungssatzAbleitung.PRIVATEINLAGE);
    }

    @Test
    @DisplayName("PRIVATENTNAHME: Soll=Privatentnahme, Haben=Kasse")
    void privatentnahmeOhneSachkonto() {
        Beleg b = beleg(BelegKategorie.PRIVATENTNAHME, null);

        BuchungssatzAbleitung.Buchungssatz bs = BuchungssatzAbleitung.ableiten(b);

        assertThat(bs.soll()).isEqualTo(BuchungssatzAbleitung.PRIVATENTNAHME);
        assertThat(bs.haben()).isEqualTo(BuchungssatzAbleitung.KASSE);
    }

    @Test
    @DisplayName("BANK / KREDITKARTE / SONSTIGER_BELEG: Soll=Haben=?")
    void nichtKasse_FragezeichenSollUndHaben() {
        for (BelegKategorie k : new BelegKategorie[]{
                BelegKategorie.BANK, BelegKategorie.KREDITKARTE,
                BelegKategorie.SONSTIGER_BELEG, BelegKategorie.UNZUGEORDNET}) {
            BuchungssatzAbleitung.Buchungssatz bs =
                    BuchungssatzAbleitung.ableiten(beleg(k, null));
            assertThat(bs.soll()).as("Soll fuer " + k).isEqualTo(BuchungssatzAbleitung.UNKLAR);
            assertThat(bs.haben()).as("Haben fuer " + k).isEqualTo(BuchungssatzAbleitung.UNKLAR);
        }
    }

    @Test
    @DisplayName("Null-Beleg: liefert ? / ? statt NPE")
    void nullBeleg_FragezeichenSollUndHaben() {
        BuchungssatzAbleitung.Buchungssatz bs = BuchungssatzAbleitung.ableiten(null);
        assertThat(bs.soll()).isEqualTo(BuchungssatzAbleitung.UNKLAR);
        assertThat(bs.haben()).isEqualTo(BuchungssatzAbleitung.UNKLAR);
    }

    private static Beleg beleg(BelegKategorie kategorie, Sachkonto sachkonto) {
        Beleg b = new Beleg();
        b.setBelegKategorie(kategorie);
        b.setSachkonto(sachkonto);
        return b;
    }

    private static Sachkonto sachkonto(String nummer, String bezeichnung, SachkontoTyp typ) {
        Sachkonto sk = new Sachkonto();
        sk.setNummer(nummer);
        sk.setBezeichnung(bezeichnung);
        sk.setKontoTyp(typ);
        return sk;
    }
}
