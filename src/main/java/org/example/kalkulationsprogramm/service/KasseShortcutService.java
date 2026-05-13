package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kalkulationsprogramm.domain.Beleg;
import org.example.kalkulationsprogramm.domain.BelegKategorie;
import org.example.kalkulationsprogramm.domain.BelegKiAnalyseStatus;
import org.example.kalkulationsprogramm.domain.BelegStatus;
import org.example.kalkulationsprogramm.domain.KasseEinstellung;
import org.example.kalkulationsprogramm.domain.Kostenstelle;
import org.example.kalkulationsprogramm.domain.Mitarbeiter;
import org.example.kalkulationsprogramm.domain.Sachkonto;
import org.example.kalkulationsprogramm.repository.BelegRepository;
import org.example.kalkulationsprogramm.repository.KasseEinstellungRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bequemlichkeits-Shortcuts fuer das Kassenbuch — vermeidet, dass der Handwerker
 * im Editor jedes Mal mehrere Konten und Kategorien zusammenstellen muss.
 *
 * Vier Shortcuts:
 *   - bankAbhebung()   : "Vater hebt Geld bei der Bank ab" -> Kassen-Eingang
 *                        (Aktivtausch Bank an Kasse; Bank-Seite wird im
 *                        Bank-Modul separat erfasst).
 *   - privatEinlage()  : Inhaber legt Bargeld in die Firma ein.
 *   - privatEntnahme() : Inhaber nimmt Bargeld aus der Firma heraus.
 *   - lohnZahlung()    : Bar-Lohnauszahlung. Wenn die Kasse danach unter den
 *                        Mindestbestand fiele, wird AUTOMATISCH vorher eine
 *                        Privateinlage in genau passender Hoehe gebucht
 *                        (Ziel: am Monatsende so wenig Bar in der Kasse wie
 *                        noetig).
 *
 * Alle Shortcuts erzeugen sofort validierte Belege ohne Datei
 * (istUmbuchung=true).
 *
 * Bewusste Race-Condition: In einem Handwerker-Single-User-Setup laufen
 * Saldo-Pruefung und save() unter READ_COMMITTED ohne expliziten Row-Lock.
 * Theoretisch koennten zwei zeitgleiche Privatentnahmen den Saldo unter
 * den Mindestbestand druecken. In der Praxis (1-2 Buchhalter, kein
 * paralleler Mass-Import) akzeptiert — falls jemals relevant, koennte ein
 * SELECT ... FOR UPDATE auf kasse_einstellung-Row als Serialisierung dienen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KasseShortcutService {

    private final BelegRepository belegRepository;
    private final KasseEinstellungRepository kasseEinstellungRepository;
    private final KasseSaldoService kasseSaldoService;

    @Transactional
    public Beleg bankAbhebung(BigDecimal betrag, LocalDate datum, String belegNr,
                              String beschreibung, Mitarbeiter ersteller) {
        assertPositiv(betrag, "Betrag");
        assertNichtNull(datum, "Datum");

        Beleg b = baseBeleg(BelegKategorie.KASSE_EINNAHME, datum, betrag, ersteller);
        b.setBelegNummer(belegNr);
        b.setBeschreibung(beschreibung != null && !beschreibung.isBlank()
                ? beschreibung : "Bank-Abhebung");
        return belegRepository.save(b);
    }

    @Transactional
    public Beleg privatEinlage(BigDecimal betrag, LocalDate datum, String beschreibung,
                                Mitarbeiter ersteller) {
        assertPositiv(betrag, "Betrag");
        assertNichtNull(datum, "Datum");

        Beleg b = baseBeleg(BelegKategorie.PRIVATEINLAGE, datum, betrag, ersteller);
        b.setBeschreibung(beschreibung != null && !beschreibung.isBlank()
                ? beschreibung : "Privateinlage");
        ladeEinstellung().map(KasseEinstellung::getPrivateinlageSachkonto)
                .ifPresent(b::setSachkonto);
        return belegRepository.save(b);
    }

    @Transactional
    public Beleg privatEntnahme(BigDecimal betrag, LocalDate datum, String beschreibung,
                                 Mitarbeiter ersteller) {
        assertPositiv(betrag, "Betrag");
        assertNichtNull(datum, "Datum");

        BigDecimal projiziert = kasseSaldoService.projiziereSaldo(
                null, null, BelegKategorie.PRIVATENTNAHME, betrag);
        kasseSaldoService.assertSaldoMindestensMindestbestand(projiziert);

        Beleg b = baseBeleg(BelegKategorie.PRIVATENTNAHME, datum, betrag, ersteller);
        b.setBeschreibung(beschreibung != null && !beschreibung.isBlank()
                ? beschreibung : "Privatentnahme");
        return belegRepository.save(b);
    }

    /**
     * Bar-Lohnauszahlung. Wenn die Kasse danach unter den Mindestbestand fiele,
     * wird vorher automatisch eine Privateinlage gebucht — Differenz so, dass
     * Saldo nach Lohn == Mindestbestand.
     */
    @Transactional
    public LohnZahlungResult lohnZahlung(BigDecimal betrag, LocalDate datum,
                                         String empfaengerName, Sachkonto sachkonto,
                                         Kostenstelle kostenstelle, Mitarbeiter ersteller) {
        assertPositiv(betrag, "Betrag");
        assertNichtNull(datum, "Datum");
        if (sachkonto == null) {
            throw new IllegalArgumentException("Sachkonto fuer Lohn-Buchung fehlt");
        }

        BigDecimal mindestbestand = kasseSaldoService.getMindestbestand();
        BigDecimal aktuellerSaldo = kasseSaldoService.berechneAktuellenSaldo();
        BigDecimal saldoNachLohn = aktuellerSaldo.subtract(betrag);

        Beleg einlage = null;
        if (saldoNachLohn.compareTo(mindestbestand) < 0) {
            BigDecimal benoetigteEinlage = mindestbestand.subtract(saldoNachLohn)
                    .setScale(2, RoundingMode.HALF_UP);
            einlage = privatEinlage(benoetigteEinlage, datum,
                    "Auto-Privateinlage fuer Lohnzahlung", ersteller);
        }

        Beleg lohn = baseBeleg(BelegKategorie.KASSE_AUSGABE, datum, betrag, ersteller);
        lohn.setBeschreibung("Lohn"
                + (empfaengerName != null && !empfaengerName.isBlank() ? " " + empfaengerName : ""));
        lohn.setSachkonto(sachkonto);
        if (kostenstelle != null) {
            lohn.setKostenstelle(kostenstelle);
        }
        Beleg lohnGespeichert = belegRepository.save(lohn);

        BigDecimal neuerSaldo = kasseSaldoService.berechneAktuellenSaldo();
        return new LohnZahlungResult(einlage, lohnGespeichert, neuerSaldo);
    }

    private Beleg baseBeleg(BelegKategorie kategorie, LocalDate datum, BigDecimal betrag,
                             Mitarbeiter ersteller) {
        Beleg b = new Beleg();
        b.setBelegKategorie(kategorie);
        b.setStatus(BelegStatus.VALIDIERT);
        b.setKiAnalyseStatus(BelegKiAnalyseStatus.DONE);
        b.setIstUmbuchung(true);
        b.setBelegDatum(datum);
        b.setBetragBrutto(betrag);
        b.setUploadDatum(LocalDateTime.now());
        b.setValidiertAm(LocalDateTime.now());
        b.setUploadedBy(ersteller);
        b.setValidiertVon(ersteller);
        return b;
    }

    private java.util.Optional<KasseEinstellung> ladeEinstellung() {
        return kasseEinstellungRepository.findSingleton();
    }

    private static void assertPositiv(BigDecimal v, String feld) {
        if (v == null || v.signum() <= 0) {
            throw new IllegalArgumentException(feld + " fehlt oder ist nicht positiv");
        }
    }

    private static void assertNichtNull(Object v, String feld) {
        if (v == null) {
            throw new IllegalArgumentException(feld + " fehlt");
        }
    }

    public record LohnZahlungResult(Beleg privateinlage, Beleg lohnBeleg, BigDecimal neuerSaldo) {}
}
