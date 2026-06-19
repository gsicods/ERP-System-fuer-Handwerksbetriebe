package org.example.kalkulationsprogramm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kalkulationsprogramm.domain.*;
import org.example.kalkulationsprogramm.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private static final String MARKER_KEY = "demo.data.seeded";

    private final Environment environment;
    private final SystemSettingRepository systemSettingRepository;
    private final FirmeninformationRepository firmeninformationRepository;
    private final AbteilungRepository abteilungRepository;
    private final ArbeitsgangRepository arbeitsgangRepository;
    private final ProduktkategorieRepository produktkategorieRepository;
    private final WerkstoffRepository werkstoffRepository;
    private final KategorieRepository kategorieRepository;
    private final ArtikelRepository artikelRepository;
    private final LieferantenRepository lieferantenRepository;
    private final KundeRepository kundeRepository;
    private final MitarbeiterRepository mitarbeiterRepository;
    private final ProjektRepository projektRepository;
    private final AnfrageRepository anfrageRepository;
    private final AusgangsGeschaeftsDokumentRepository dokumentRepository;
    private final EmailRepository emailRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!isH2Profile()) {
            return;
        }
        if (systemSettingRepository.existsById(MARKER_KEY)) {
            return;
        }
        if (kundeRepository.count() > 0 && projektRepository.count() > 0) {
            markSeeded();
            return;
        }

        seedCompany();

        Abteilung buero = abteilung("Buro", true, true, true, true);
        Abteilung montage = abteilung("Montage", false, false, false, false);
        Abteilung werkstatt = abteilung("Werkstatt", false, false, false, false);
        Abteilung buchhaltung = abteilung("Buchhaltung", true, true, false, false);

        Arbeitsgang aufmass = arbeitsgang("Aufmass beim Kunden", buero);
        Arbeitsgang planung = arbeitsgang("Planung und Kalkulation", buero);
        Arbeitsgang fertigung = arbeitsgang("Werkstattfertigung", werkstatt);
        Arbeitsgang montageGang = arbeitsgang("Montage vor Ort", montage);
        Arbeitsgang abnahme = arbeitsgang("Abnahme und Nacharbeit", montage);

        Produktkategorie fenster = produktkategorie("Fenster", Verrechnungseinheit.STUECK);
        Produktkategorie tuer = produktkategorie("Turen", Verrechnungseinheit.STUECK);
        Produktkategorie fassade = produktkategorie("Fassade", Verrechnungseinheit.QUADRATMETER);
        Produktkategorie service = produktkategorie("Servicearbeiten", Verrechnungseinheit.STUECK);

        Werkstoff aluminium = werkstoff("Aluminium");
        Werkstoff stahl = werkstoff("Stahl");
        Kategorie profile = kategorie("Profile und Bleche");
        artikel("Heroal Fensterprofil W72", "Fenstersystem mit thermischer Trennung", "Stuck", fenster, profile, aluminium);
        artikel("Stahl-Unterkonstruktion verzinkt", "Tragprofil fur Fassadenmontage", "lfm", fassade, profile, stahl);
        artikel("Turdruecker Edelstahl", "Beschlagset fur Objekttueren", "Set", tuer, profile, stahl);

        Lieferanten wuerth = lieferant("Wurth Niederlassung Hannover", "Befestigungstechnik", "Hannover", "materials@example.local");
        Lieferanten heroal = lieferant("Heroal Systeme GmbH", "Profilsysteme", "Verl", "vertrieb@example.local");
        Lieferanten glasNord = lieferant("Glas Nord GmbH", "Glaslieferant", "Hamburg", "dispo@example.local");

        Kunde mueller = kunde("K-10001", Anrede.FAMILIE, "Familie Muller", "Sabine Muller", "Hauptstrasse 18", "30159", "Hannover", "mueller@example.local");
        Kunde cityBau = kunde("K-10002", Anrede.FIRMA, "CityBau Projekt GmbH", "Thomas Weber", "Marktallee 7", "30539", "Hannover", "weber@example.local");
        Kunde praxis = kunde("K-10003", Anrede.FIRMA, "Praxis Dr. Schneider", "Dr. Anna Schneider", "Bahnhofstrasse 4", "31134", "Hildesheim", "praxis@example.local");
        Kunde hausverwaltung = kunde("K-10004", Anrede.FIRMA, "Hausverwaltung Becker", "Miriam Becker", "Lindenweg 22", "29221", "Celle", "becker@example.local");

        Mitarbeiter max = mitarbeiter("Max", "Keller", "max.keller@example.local", Qualifikation.MEISTER, new BigDecimal("42.50"), buero, montage);
        Mitarbeiter lena = mitarbeiter("Lena", "Hoffmann", "lena.hoffmann@example.local", Qualifikation.FACHARBEITER, new BigDecimal("31.00"), montage, werkstatt);
        Mitarbeiter emir = mitarbeiter("Emir", "Basic", "emir.basic@example.local", Qualifikation.FACHARBEITER, new BigDecimal("29.50"), werkstatt, montage);
        Mitarbeiter nina = mitarbeiter("Nina", "Voss", "nina.voss@example.local", Qualifikation.FACHARBEITER, new BigDecimal("28.00"), buchhaltung, buero);

        Projekt p1 = projekt("EFH Muller - Fenster und Haustur", "A-2026-0001", mueller, "Hauptstrasse 18", "30159", "Hannover", ProjektArt.PAUSCHAL, new BigDecimal("28650.00"), false, false, LocalDate.now().minusDays(34));
        Projekt p2 = projekt("CityBau - Fassadenrevision Bauteil B", "A-2026-0002", cityBau, "Expo Plaza 3", "30539", "Hannover", ProjektArt.REGIE, new BigDecimal("74200.00"), false, false, LocalDate.now().minusDays(18));
        Projekt p3 = projekt("Praxis Schneider - Eingangsanlage", "A-2026-0003", praxis, "Bahnhofstrasse 4", "31134", "Hildesheim", ProjektArt.PAUSCHAL, new BigDecimal("18490.00"), true, true, LocalDate.now().minusDays(72));
        Projekt p4 = projekt("Hausverwaltung Becker - Wartung Treppenhaus", "A-2026-0004", hausverwaltung, "Lindenweg 22", "29221", "Celle", ProjektArt.GARANTIE, new BigDecimal("0.00"), false, false, LocalDate.now().minusDays(6));

        addCategory(p1, fenster, "12.00");
        addCategory(p1, tuer, "1.00");
        addCategory(p2, fassade, "148.50");
        addCategory(p3, tuer, "2.00");
        addCategory(p4, service, "1.00");

        addMaterial(p1, heroal, "Fensterprofile und Beschlage", "HE-26041", 6, "9450.00");
        addMaterial(p1, glasNord, "3-fach Isolierglas", "GL-90312", 6, "6120.00");
        addMaterial(p2, wuerth, "Befestigungsmittel Fassade", "WU-48110", 6, "1280.00");
        addMaterial(p3, heroal, "Automatik-Tueranlage", "HE-24118", 4, "7350.00");

        addTime(p1, max, aufmass, "5.00", 31, "Aufmass und Kundentermin");
        addTime(p1, lena, fertigung, "18.50", 24, "Rahmen vorbereitet");
        addTime(p1, emir, montageGang, "22.00", 12, "Fenstermontage EG");
        addTime(p2, max, planung, "8.00", 17, "Kalkulation Regieauftrag");
        addTime(p2, lena, montageGang, "16.00", 8, "Fassade geoffnet und gepruft");
        addTime(p3, emir, montageGang, "14.50", 50, "Eingangsanlage montiert");
        addTime(p3, max, abnahme, "2.00", 44, "Abnahme mit Kundin");
        addTime(p4, nina, planung, "1.50", 5, "Termin und Material koordiniert");

        Anfrage a1 = anfrage("Wintergarten Erweiterung Richter", mueller, "Nebenstrasse 8", "30161", "Hannover", new BigDecimal("32500.00"), false);
        Anfrage a2 = anfrage("Burotrennwand Glas - CityBau", cityBau, "Expo Plaza 3", "30539", "Hannover", new BigDecimal("21800.00"), false);
        Anfrage a3 = anfrage("Wartungsvertrag Praxis", praxis, "Bahnhofstrasse 4", "31134", "Hildesheim", new BigDecimal("2400.00"), true);

        dokument("2026/06/00001", AusgangsGeschaeftsDokumentTyp.ANGEBOT, p1, null, mueller, "Angebot Fenster und Haustur", "24075.63", false);
        dokument("2026/06/00002", AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG, p1, null, mueller, "Auftragsbestatigung Fenster und Haustur", "24075.63", true);
        dokument("2026/06/00003", AusgangsGeschaeftsDokumentTyp.RECHNUNG, p3, null, praxis, "Rechnung Eingangsanlage", "15537.82", true);
        dokument("2026/06/00004", AusgangsGeschaeftsDokumentTyp.ANGEBOT, null, a1, mueller, "Angebot Wintergarten Erweiterung", "27310.92", false);
        dokument("2026/06/00005", AusgangsGeschaeftsDokumentTyp.ANGEBOT, p2, null, cityBau, "Regieangebot Fassadenrevision", "62352.94", false);

        email("demo-001@example.local", EmailDirection.IN, "sabine.mueller@example.local", "info@demo-handwerk.local", "Bitte um Terminabstimmung", p1, null, null);
        email("demo-002@example.local", EmailDirection.OUT, "info@demo-handwerk.local", "sabine.mueller@example.local", "Auftragsbestatigung A-2026-0001", p1, null, null);
        email("demo-003@example.local", EmailDirection.IN, "weber@citybau.example.local", "info@demo-handwerk.local", "Fassade Bauteil B - Zusatzflache", p2, null, null);
        email("demo-004@example.local", EmailDirection.IN, "vertrieb@heroal.example.local", "einkauf@demo-handwerk.local", "Liefertermin Fensterprofile", null, null, heroal);
        email("demo-005@example.local", EmailDirection.IN, "richter@example.local", "info@demo-handwerk.local", "Anfrage Wintergarten", null, a1, null);

        markSeeded();
        log.info("Demo-Daten wurden angelegt.");
    }

    private boolean isH2Profile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("h2");
    }

    private void markSeeded() {
        systemSettingRepository.save(new SystemSetting(MARKER_KEY, "true", "Demo-Daten wurden angelegt"));
    }

    private void seedCompany() {
        if (firmeninformationRepository.existsById(1L)) {
            return;
        }
        Firmeninformation info = new Firmeninformation();
        info.setFirmenname("Demo Metallbau GmbH");
        info.setStrasse("Werkstrasse 12");
        info.setPlz("30165");
        info.setOrt("Hannover");
        info.setTelefon("+49 511 123456");
        info.setEmail("info@demo-handwerk.local");
        info.setWebsite("https://demo-handwerk.local");
        info.setSteuernummer("25/123/45678");
        info.setUstIdNr("DE123456789");
        info.setBankName("Demo Bank");
        info.setIban("DE44500105175407324931");
        info.setBic("DEMODEFFXXX");
        info.setGeschaeftsfuehrer("Goran Demo");
        info.setFusszeileText("Demo Metallbau GmbH - Meisterbetrieb fur Fenster, Turen und Fassaden");
        firmeninformationRepository.save(info);
    }

    private Abteilung abteilung(String name, boolean genehmigen, boolean sehen, boolean freigabe, boolean web) {
        Abteilung a = new Abteilung();
        a.setName(name);
        a.setDarfRechnungenGenehmigen(genehmigen);
        a.setDarfRechnungenSehen(sehen);
        a.setDarfFreigabeAnnahmePushen(freigabe);
        a.setDarfWebseitenAnfragenPushen(web);
        return abteilungRepository.save(a);
    }

    private Arbeitsgang arbeitsgang(String beschreibung, Abteilung abteilung) {
        Arbeitsgang a = new Arbeitsgang();
        a.setBeschreibung(beschreibung);
        a.setAbteilung(abteilung);
        return arbeitsgangRepository.save(a);
    }

    private Produktkategorie produktkategorie(String name, Verrechnungseinheit einheit) {
        Produktkategorie p = new Produktkategorie();
        p.setBezeichnung(name);
        p.setBeschreibung("Demo-Kategorie " + name);
        p.setVerrechnungseinheit(einheit);
        return produktkategorieRepository.save(p);
    }

    private Werkstoff werkstoff(String name) {
        Werkstoff w = new Werkstoff();
        w.setName(name);
        return werkstoffRepository.save(w);
    }

    private Kategorie kategorie(String beschreibung) {
        Kategorie k = new Kategorie();
        k.setBeschreibung(beschreibung);
        return kategorieRepository.save(k);
    }

    private void artikel(String name, String text, String preiseinheit, Produktkategorie produktkategorie, Kategorie kategorie, Werkstoff werkstoff) {
        Artikel a = new Artikel();
        a.setProduktlinie(produktkategorie.getBezeichnung());
        a.setProduktname(name);
        a.setProdukttext(text);
        a.setPreiseinheit(preiseinheit);
        a.setVerpackungseinheit(1L);
        a.setVerrechnungseinheit(produktkategorie.getVerrechnungseinheit());
        a.setKategorie(kategorie);
        a.setWerkstoff(werkstoff);
        artikelRepository.save(a);
    }

    private Lieferanten lieferant(String name, String typ, String ort, String email) {
        Lieferanten l = new Lieferanten();
        l.setLieferantenname(name);
        l.setLieferantenTyp(typ);
        l.setOrt(ort);
        l.setStrasse("Industriestrasse 1");
        l.setPlz("30000");
        l.setTelefon("+49 511 1000");
        l.setVertreter("Demo Vertrieb");
        l.setIstAktiv(true);
        l.setStartZusammenarbeit(new Date());
        l.setEigeneKundennummer("DK-" + Math.abs(name.hashCode() % 10000));
        l.getKundenEmails().add(email);
        return lieferantenRepository.save(l);
    }

    private Kunde kunde(String nr, Anrede anrede, String name, String kontakt, String strasse, String plz, String ort, String email) {
        Kunde k = new Kunde();
        k.setKundennummer(nr);
        k.setAnrede(anrede);
        k.setName(name);
        k.setAnsprechspartner(kontakt);
        k.setStrasse(strasse);
        k.setPlz(plz);
        k.setOrt(ort);
        k.setTelefon("+49 511 " + nr.substring(nr.length() - 5));
        k.setZahlungsziel(14);
        k.getKundenEmails().add(email);
        return kundeRepository.save(k);
    }

    private Mitarbeiter mitarbeiter(String vorname, String nachname, String email, Qualifikation qualifikation, BigDecimal stundenlohn, Abteilung... abteilungen) {
        Mitarbeiter m = new Mitarbeiter();
        m.setVorname(vorname);
        m.setNachname(nachname);
        m.setEmail(email);
        m.setTelefon("+49 511 555");
        m.setStrasse("Mitarbeiterweg 5");
        m.setPlz("30165");
        m.setOrt("Hannover");
        m.setQualifikation(qualifikation);
        m.setEintrittsdatum(LocalDate.now().minusYears(2));
        m.setGeburtstag(LocalDate.now().minusYears(35));
        m.setAktiv(true);
        m.setStundenlohn(stundenlohn);
        m.setBeschaeftigungsart(Beschaeftigungsart.REGULAER);
        m.setJahresUrlaub(28);
        m.setResturlaubVorjahr(2);
        m.setLoginToken("demo-" + vorname.toLowerCase() + "-" + nachname.toLowerCase());
        m.getAbteilungen().addAll(List.of(abteilungen));
        return mitarbeiterRepository.save(m);
    }

    private Projekt projekt(String name, String nr, Kunde kunde, String strasse, String plz, String ort, ProjektArt art, BigDecimal brutto, boolean bezahlt, boolean abgeschlossen, LocalDate anlage) {
        Projekt p = new Projekt();
        p.setBauvorhaben(name);
        p.setAuftragsnummer(nr);
        p.setKundenId(kunde);
        p.setStrasse(strasse);
        p.setPlz(plz);
        p.setOrt(ort);
        p.setProjektArt(art);
        p.setBruttoPreis(brutto);
        p.setBezahlt(bezahlt);
        p.setAbgeschlossen(abgeschlossen);
        p.setAnlegedatum(anlage);
        p.setAbschlussdatum(abgeschlossen ? anlage.plusDays(28) : null);
        p.setKurzbeschreibung("Demo-Projekt mit kalkulierten Positionen, Materialkosten und Zeitbuchungen.");
        p.getKundenEmails().addAll(kunde.getKundenEmails());
        return projektRepository.save(p);
    }

    private void addCategory(Projekt projekt, Produktkategorie produktkategorie, String menge) {
        ProjektProduktkategorie ppk = new ProjektProduktkategorie();
        ppk.setProjekt(projekt);
        ppk.setProduktkategorie(produktkategorie);
        ppk.setMenge(new BigDecimal(menge));
        projekt.getProjektProduktkategorien().add(ppk);
        projektRepository.save(projekt);
    }

    private void addMaterial(Projekt projekt, Lieferanten lieferant, String text, String artikelnummer, int monat, String betrag) {
        Materialkosten mk = new Materialkosten();
        mk.setProjekt(projekt);
        mk.setLieferant(lieferant);
        mk.setBeschreibung(text);
        mk.setExterneArtikelnummer(artikelnummer);
        mk.setMonat(monat);
        mk.setBetrag(new BigDecimal(betrag));
        mk.setRechnungsnummer("ER-" + artikelnummer);
        projekt.getMaterialkosten().add(mk);
        projektRepository.save(projekt);
    }

    private void addTime(Projekt projekt, Mitarbeiter mitarbeiter, Arbeitsgang gang, String stunden, int daysAgo, String notiz) {
        Zeitbuchung z = new Zeitbuchung();
        LocalDateTime start = LocalDateTime.now().minusDays(daysAgo).withHour(8).withMinute(0).withSecond(0).withNano(0);
        BigDecimal dauer = new BigDecimal(stunden);
        z.setProjekt(projekt);
        z.setMitarbeiter(mitarbeiter);
        z.setErfasstVon(mitarbeiter);
        z.setArbeitsgang(gang);
        z.setStartZeit(start);
        z.setEndeZeit(start.plusMinutes(dauer.multiply(new BigDecimal("60")).longValue()));
        z.setAnzahlInStunden(dauer);
        z.setNotiz(notiz);
        z.setTyp(BuchungsTyp.ARBEIT);
        z.setErfasstVia(ErfassungsQuelle.DESKTOP);
        z.setErfasstAm(LocalDateTime.now().minusDays(daysAgo));
        z.setIdempotencyKey(UUID.nameUUIDFromBytes(("demo-" + projekt.getAuftragsnummer() + "-" + mitarbeiter.getEmail() + "-" + daysAgo).getBytes(StandardCharsets.UTF_8)).toString());
        projekt.getZeitbuchungen().add(z);
        projektRepository.save(projekt);
    }

    private Anfrage anfrage(String name, Kunde kunde, String strasse, String plz, String ort, BigDecimal betrag, boolean abgeschlossen) {
        Anfrage a = new Anfrage();
        a.setBauvorhaben(name);
        a.setKunde(kunde);
        a.setProjektStrasse(strasse);
        a.setProjektPlz(plz);
        a.setProjektOrt(ort);
        a.setBetrag(betrag);
        a.setAbgeschlossen(abgeschlossen);
        a.setAnlegedatum(LocalDate.now().minusDays(abgeschlossen ? 40 : 9));
        a.setKurzbeschreibung("Demo-Anfrage aus Website oder E-Mail Eingang.");
        a.getKundenEmails().addAll(kunde.getKundenEmails());
        return anfrageRepository.save(a);
    }

    private void dokument(String nummer, AusgangsGeschaeftsDokumentTyp typ, Projekt projekt, Anfrage anfrage, Kunde kunde, String betreff, String netto, boolean gebucht) {
        AusgangsGeschaeftsDokument d = new AusgangsGeschaeftsDokument();
        d.setDokumentNummer(nummer);
        d.setTyp(typ);
        d.setDatum(LocalDate.now().minusDays(6));
        d.setProjekt(projekt);
        d.setAnfrage(anfrage);
        d.setKunde(kunde);
        d.setBetreff(betreff);
        d.setBetragNetto(new BigDecimal(netto));
        d.setMwstSatz(new BigDecimal("0.19"));
        d.setBetragBrutto(new BigDecimal(netto).multiply(new BigDecimal("1.19")));
        d.setZahlungszielTage(14);
        d.setVersandDatum(LocalDate.now().minusDays(5));
        d.setGebucht(gebucht);
        d.setGebuchtAm(gebucht ? LocalDate.now().minusDays(4) : null);
        d.setHtmlInhalt("<p>Demo-Dokument fur " + betreff + "</p>");
        d.setPositionenJson("[]");
        dokumentRepository.save(d);
    }

    private void email(String messageId, EmailDirection direction, String from, String to, String subject, Projekt projekt, Anfrage anfrage, Lieferanten lieferant) {
        Email e = new Email();
        e.setMessageId(messageId);
        e.setDirection(direction);
        e.setFromAddress(from);
        e.setRecipient(to);
        e.setSubject(subject);
        e.setBody("Demo-E-Mail: " + subject);
        e.setHtmlBody("<p>Demo-E-Mail: " + subject + "</p>");
        e.setSentAt(LocalDateTime.now().minusDays(3));
        e.setRead(direction == EmailDirection.OUT);
        e.setProcessingStatus(EmailProcessingStatus.DONE);
        e.setProcessedAt(LocalDateTime.now().minusDays(3));
        if (projekt != null) {
            e.assignToProjekt(projekt);
        } else if (anfrage != null) {
            e.assignToAnfrage(anfrage);
        } else if (lieferant != null) {
            e.assignToLieferant(lieferant);
        }
        e.extractSenderDomain();
        emailRepository.save(e);
    }
}
