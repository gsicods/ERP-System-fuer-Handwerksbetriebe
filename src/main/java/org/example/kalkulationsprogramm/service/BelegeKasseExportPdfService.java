package org.example.kalkulationsprogramm.service;

import com.lowagie.text.*;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.IOException;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.domain.Beleg;
import org.example.kalkulationsprogramm.domain.BelegKategorie;
import org.example.kalkulationsprogramm.domain.BelegStatus;
import org.example.kalkulationsprogramm.domain.Firmeninformation;
import org.example.kalkulationsprogramm.domain.Sachkonto;
import org.example.kalkulationsprogramm.domain.SachkontoTyp;
import org.example.kalkulationsprogramm.repository.BelegRepository;
import org.example.kalkulationsprogramm.repository.FirmeninformationRepository;
import org.example.kalkulationsprogramm.repository.SachkontoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Erzeugt einen Monatsexport der Buchhaltung als PDF — geplant fuer die
 * Uebergabe an den Steuerberater (ein PDF pro Monat, zusammen mit dem Ordner
 * der hochgeladenen Belegfotos).
 *
 * Inhalt:
 *  - Kopf mit Logo, Zeitraum, Erstelldatum
 *  - KPI-Box (Einnahmen, Ausgaben, Privatentnahmen/-einlagen, Saldo Kasse)
 *  - Auswertung pro Sachkonto (analog GET /api/buchhaltung/auswertung)
 *  - Liste aller validierten Belege im Monat
 *
 * Optisch an {@link ProjektAuswertungPdfService} angelehnt (rose-600 Header,
 * helle Zebra-Streifen, dezente Borders).
 */
@Service
@RequiredArgsConstructor
public class BelegeKasseExportPdfService {

    private final BelegRepository belegRepository;
    private final SachkontoRepository sachkontoRepository;
    private final FirmeninformationRepository firmeninformationRepository;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    private static final Color HEADER_BG   = new Color(220, 38, 38);   // rose-600
    private static final Color ROW_ALT     = new Color(254, 242, 242); // rose-50
    private static final Color BORDER      = new Color(229, 231, 235); // slate-200
    private static final Color SUM_BG      = new Color(241, 245, 249); // slate-100
    private static final Color TEXT_DARK   = new Color(30, 41, 59);    // slate-800
    private static final Color TEXT_MUTED  = new Color(100, 116, 139); // slate-500
    private static final Color TEXT_CELL   = new Color(55, 65, 81);    // slate-700
    private static final Color FOOTER_GREY = new Color(148, 163, 184); // slate-400
    private static final Color KPI_BG      = new Color(254, 242, 242); // rose-50
    private static final Color KPI_ACCENT  = new Color(220, 38, 38);

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd.MM.yy");

    /**
     * @param jahr  vierstellig (z.B. 2026)
     * @param monat 1..12
     */
    public Path generatePdf(int jahr, int monat) {
        YearMonth ym = YearMonth.of(jahr, monat);
        LocalDate von = ym.atDay(1);
        LocalDate bis = ym.atEndOfMonth();

        List<Beleg> alle = belegRepository.findByStatusOrderByUploadDatumDesc(BelegStatus.VALIDIERT);
        List<Beleg> imMonat = alle.stream()
                .filter(b -> b.getBelegDatum() != null
                        && !b.getBelegDatum().isBefore(von)
                        && !b.getBelegDatum().isAfter(bis))
                .sorted(Comparator
                        .comparing(Beleg::getBelegDatum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Beleg::getId))
                .toList();

        try {
            // Temp-PDF unter dem konfigurierten upload.path ablegen — sonst
            // driftet die Temp-Location, wenn das Upload-Verzeichnis (z.B. in
            // application-local.properties) umgebogen wurde.
            Path dir = Paths.get(uploadPath);
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, "belege-export-", ".pdf");
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, Files.newOutputStream(temp));
            doc.open();

            // Kopfzeile: Firmenlogo links, Firmeninformationen rechts.
            Firmeninformation firma = firmeninformationRepository.findFirmeninformation().orElse(null);
            addBriefkopf(doc, firma);

            addTitle(doc, ym);
            addKpiSection(doc, imMonat);
            addSachkontoAuswertung(doc, imMonat);
            addBelegListe(doc, imMonat);
            addFooter(doc);

            doc.close();
            return temp;

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Erzeugen des Belege-Monatsexports", e);
        }
    }

    // ===================== Sections =====================

    /**
     * Briefkopf mit Firmenlogo (aus {@code uploads/firma/logo/<logoDateiname>})
     * links und Firmenstammdaten (Name, Adresse, Kontakt, Steuernummern) rechts.
     * Wird oben in das Dokument gerendert, bevor der eigentliche Titel kommt —
     * der Steuerberater sieht damit auf einen Blick, von welcher Firma der
     * Export stammt.
     *
     * Fallbacks:
     *  - Logo nicht gepflegt oder Datei fehlt: nur Firmen-Text rechts wird gerendert.
     *  - Keine Firmeninformation in der DB: nur das (eventuelle) Static-Logo.
     *  - Beides fehlt: stillschweigend ueberspringen — PDF bleibt valide.
     */
    private void addBriefkopf(Document doc, Firmeninformation firma) throws DocumentException {
        Image logo = ladeFirmenlogo(firma);
        if (logo == null && firma == null) {
            return; // nichts zu zeigen, Title-Section folgt direkt
        }
        PdfPTable kopf = new PdfPTable(new float[]{ 2f, 5f });
        kopf.setWidthPercentage(100);
        kopf.setSpacingAfter(8f);

        // Linke Spalte: Logo (zentriert) oder leere Zelle
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (logo != null) {
            logo.scaleToFit(140, 70);
            logoCell.addElement(logo);
        }
        kopf.addCell(logoCell);

        // Rechte Spalte: Firmenstammdaten in Handwerker-Sprache (kein Buchhalter-Jargon)
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (firma != null) {
            Font firmenname = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, TEXT_DARK);
            Font line       = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_CELL);
            Font lineMuted  = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);

            addRightLine(infoCell, firma.getFirmenname(), firmenname);
            addRightLine(infoCell, joinNonEmpty(" ", firma.getStrasse()), line);
            addRightLine(infoCell, joinNonEmpty(" ", firma.getPlz(), firma.getOrt()), line);
            // Kontakt-Zeile: Tel · E-Mail · Web (nur was gepflegt ist)
            String kontakt = joinNonEmpty(" · ",
                    prefix("Tel. ",     firma.getTelefon()),
                    prefix("",          firma.getEmail()),
                    prefix("",          firma.getWebsite()));
            addRightLine(infoCell, kontakt, lineMuted);
            // Steuer-Zeile: nur fuer Steuerberater relevant, daher klein darunter
            String steuer = joinNonEmpty(" · ",
                    prefix("St.-Nr. ",  firma.getSteuernummer()),
                    prefix("USt-IdNr. ", firma.getUstIdNr()));
            addRightLine(infoCell, steuer, lineMuted);
        }
        kopf.addCell(infoCell);

        doc.add(kopf);
    }

    /**
     * Laedt das im FirmaEditor hinterlegte Logo aus dem Upload-Verzeichnis.
     * Faellt auf die mitgelieferte Static-Resource zurueck, wenn das Firmen-Logo
     * nicht gepflegt oder die Datei nicht (mehr) vorhanden ist.
     */
    private Image ladeFirmenlogo(Firmeninformation firma) {
        String dateiname = firma != null ? firma.getLogoDateiname() : null;
        if (dateiname != null && !dateiname.isBlank()) {
            // Pfad-Traversal blocken — Defense-in-Depth, der Upload-Pfad sollte
            // den Dateinamen ohnehin sanitisieren, aber wir prüfen hier nochmal.
            String safe = dateiname.trim();
            if (!safe.contains("..") && !safe.contains("/") && !safe.contains("\\")) {
                Path base = Paths.get(uploadPath, "firma", "logo").toAbsolutePath().normalize();
                Path logoPath = base.resolve(safe).normalize();
                // Zweiter Check: nach Normalisierung muss der aufgeloeste Pfad
                // immer noch unterhalb des Logo-Verzeichnisses liegen — sonst
                // hat der Dateiname uns ueber einen exotischen Trick (z.B. NUL,
                // Unicode-Slash) doch aus dem Sandkasten herausgehoben.
                if (logoPath.startsWith(base) && Files.exists(logoPath)) {
                    try {
                        return Image.getInstance(logoPath.toString());
                    } catch (IOException | BadElementException ex) {
                        // Datei kaputt / kein gueltiges Bild — Fallback unten greift
                    }
                }
            }
        }
        // Fallback: mitgelieferte Static-Resource (kann ebenfalls fehlen).
        try {
            java.net.URL url = getClass().getResource("/static/firmenlogo_icon.png");
            if (url != null) return Image.getInstance(url);
        } catch (IOException | BadElementException ignored) {
            // Kein Logo verfuegbar — PDF wird ohne Logo gerendert.
        }
        return null;
    }

    private void addRightLine(PdfPCell cell, String text, Font font) {
        if (text == null || text.isBlank()) return;
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(p);
    }

    private String joinNonEmpty(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private String prefix(String prefix, String value) {
        if (value == null || value.isBlank()) return null;
        return prefix + value.trim();
    }

    private void addTitle(Document doc, YearMonth ym) throws DocumentException {
        Font titleFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TEXT_DARK);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, TEXT_MUTED);
        Font kategorie    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, KPI_ACCENT);

        Paragraph kat = new Paragraph("BUCHHALTUNG", kategorie);
        doc.add(kat);

        doc.add(new Paragraph("BELEGE & KASSE – MONATSEXPORT", titleFont));

        String zeitraum = "Zeitraum: " + monatLabel(ym)
                + "  ·  Erstellt am " + LocalDate.now().format(DATE_FMT);
        doc.add(new Paragraph(zeitraum, subTitleFont));
        doc.add(new Paragraph(" "));
    }

    private void addKpiSection(Document doc, List<Beleg> belege) throws DocumentException {
        BigDecimal sumKasseEin = sumKategorie(belege, BelegKategorie.KASSE_EINNAHME);
        BigDecimal sumKasseAus = sumKategorie(belege, BelegKategorie.KASSE_AUSGABE);
        BigDecimal sumPrivEnt  = sumKategorie(belege, BelegKategorie.PRIVATENTNAHME);
        BigDecimal sumPrivEin  = sumKategorie(belege, BelegKategorie.PRIVATEINLAGE);
        BigDecimal sumBank     = sumKategorie(belege, BelegKategorie.BANK);
        BigDecimal sumKredit   = sumKategorie(belege, BelegKategorie.KREDITKARTE);

        PdfPTable kpi = new PdfPTable(4);
        kpi.setWidthPercentage(100);
        kpi.setSpacingBefore(4f);

        kpi.addCell(kpiCell("Kasse Einnahmen", sumKasseEin));
        kpi.addCell(kpiCell("Kasse Ausgaben",  sumKasseAus));
        kpi.addCell(kpiCell("Privatentnahmen", sumPrivEnt));
        kpi.addCell(kpiCell("Privateinlagen",  sumPrivEin));
        kpi.addCell(kpiCell("Bank-Belege",     sumBank));
        kpi.addCell(kpiCell("Kreditkarte",     sumKredit));
        kpi.addCell(kpiCell("Belege gesamt",   BigDecimal.valueOf(belege.size()), false));
        kpi.addCell(kpiCell("Saldo Kasse Monat",
                sumKasseEin.subtract(sumKasseAus).add(sumPrivEin).subtract(sumPrivEnt)));

        doc.add(kpi);
        doc.add(new Paragraph(" "));
    }

    private void addSachkontoAuswertung(Document doc, List<Beleg> belege) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, TEXT_DARK);
        Paragraph section = new Paragraph("Auswertung nach Sachkonto", sectionFont);
        section.setSpacingBefore(8f);
        doc.add(section);

        // Gruppieren wie SachkontoController.auswertung()
        Map<Long, BigDecimal> summen = new HashMap<>();
        Map<Long, Integer> counts = new HashMap<>();
        BigDecimal ohneKonto = BigDecimal.ZERO;
        int ohneKontoAnzahl = 0;

        for (Beleg b : belege) {
            BigDecimal brutto = nullSafe(b.getBetragBrutto());
            if (b.getSachkonto() == null) {
                ohneKonto = ohneKonto.add(brutto);
                ohneKontoAnzahl++;
            } else {
                Long key = b.getSachkonto().getId();
                summen.merge(key, brutto, BigDecimal::add);
                counts.merge(key, 1, Integer::sum);
            }
        }

        PdfPTable t = new PdfPTable(new float[]{ 1f, 5f, 2f, 1.2f, 2f });
        t.setWidthPercentage(100);
        t.setSpacingBefore(4f);
        addHeader(t, "Nr.", "Konto", "Typ", "Belege", "Summe brutto");

        BigDecimal sumAufwand = BigDecimal.ZERO;
        BigDecimal sumErtrag  = BigDecimal.ZERO;
        BigDecimal sumPrivat  = BigDecimal.ZERO;
        BigDecimal sumNeutral = BigDecimal.ZERO;

        boolean alt = false;
        List<Sachkonto> sortierteKonten = sachkontoRepository.findAllByOrderBySortierungAscBezeichnungAsc();
        for (Sachkonto sk : sortierteKonten) {
            BigDecimal s = summen.get(sk.getId());
            if (s == null) continue;
            Color bg = alt ? ROW_ALT : Color.WHITE;
            t.addCell(cell(sk.getNummer() != null ? sk.getNummer() : "–", bg, Element.ALIGN_LEFT));
            t.addCell(cell(sk.getBezeichnung(), bg, Element.ALIGN_LEFT));
            t.addCell(cell(typLabel(sk.getKontoTyp()), bg, Element.ALIGN_LEFT));
            t.addCell(cell(String.valueOf(counts.getOrDefault(sk.getId(), 0)), bg, Element.ALIGN_RIGHT));
            t.addCell(cell(formatEuro(s) + " €", bg, Element.ALIGN_RIGHT));
            alt = !alt;
            switch (sk.getKontoTyp()) {
                case AUFWAND -> sumAufwand = sumAufwand.add(s);
                case ERTRAG  -> sumErtrag  = sumErtrag.add(s);
                case PRIVAT  -> sumPrivat  = sumPrivat.add(s);
                case NEUTRAL -> sumNeutral = sumNeutral.add(s);
            }
        }

        if (ohneKonto.signum() != 0) {
            Color bg = alt ? ROW_ALT : Color.WHITE;
            t.addCell(cell("–", bg, Element.ALIGN_LEFT));
            t.addCell(cell("(Noch keinem Konto zugeordnet)", bg, Element.ALIGN_LEFT));
            t.addCell(cell("offen", bg, Element.ALIGN_LEFT));
            t.addCell(cell(String.valueOf(ohneKontoAnzahl), bg, Element.ALIGN_RIGHT));
            t.addCell(cell(formatEuro(ohneKonto) + " €", bg, Element.ALIGN_RIGHT));
        }

        // Summenzeilen
        addSumRow(t, "Summe Erträge",   sumErtrag);
        addSumRow(t, "Summe Aufwand",   sumAufwand);
        addSumRow(t, "Summe Privat",    sumPrivat);
        if (sumNeutral.signum() != 0) {
            addSumRow(t, "Summe Neutral", sumNeutral);
        }
        addTotalRow(t, "Ergebnis (Ertrag - Aufwand)", sumErtrag.subtract(sumAufwand));

        doc.add(t);
        doc.add(new Paragraph(" "));
    }

    private void addBelegListe(Document doc, List<Beleg> belege) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, TEXT_DARK);
        Paragraph section = new Paragraph("Belege im Zeitraum (" + belege.size() + ")", sectionFont);
        section.setSpacingBefore(12f);
        doc.add(section);

        if (belege.isEmpty()) {
            Font muted = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, TEXT_MUTED);
            Paragraph empty = new Paragraph("Keine validierten Belege in diesem Monat.", muted);
            empty.setSpacingBefore(8f);
            doc.add(empty);
            return;
        }

        // Spalten: Datum | BelegNr | Kategorie | Lieferant | Sachkonto | Beschreibung | MwSt | Netto | Brutto
        PdfPTable t = new PdfPTable(new float[]{ 1.1f, 1.4f, 2f, 2.5f, 2.5f, 3.2f, 0.8f, 1.2f, 1.3f });
        t.setWidthPercentage(100);
        t.setSpacingBefore(4f);
        addHeader(t, "Datum", "Beleg-Nr.", "Kategorie", "Lieferant", "Sachkonto",
                "Beschreibung", "MwSt", "Netto €", "Brutto €");

        BigDecimal sumNetto = BigDecimal.ZERO;
        BigDecimal sumBrutto = BigDecimal.ZERO;
        boolean alt = false;
        for (Beleg b : belege) {
            Color bg = alt ? ROW_ALT : Color.WHITE;
            String datum = b.getBelegDatum() != null ? b.getBelegDatum().format(DATE_SHORT) : "–";
            String nr    = b.getBelegNummer() != null ? b.getBelegNummer() : "–";
            String kat   = kategorieLabel(b.getBelegKategorie());
            String lief  = b.getLieferant() != null ? b.getLieferant().getLieferantenname() : "–";
            String konto = b.getSachkonto() != null
                    ? (b.getSachkonto().getNummer() != null ? b.getSachkonto().getNummer() + " " : "")
                            + b.getSachkonto().getBezeichnung()
                    : "–";
            String desc  = b.getBeschreibung() != null ? b.getBeschreibung() : "";
            String mwst  = b.getMwstSatz() != null ? formatEuro(b.getMwstSatz()) + "%" : "–";
            String netto = b.getBetragNetto() != null ? formatEuro(b.getBetragNetto()) : "–";
            String brutto = b.getBetragBrutto() != null ? formatEuro(b.getBetragBrutto()) : "–";

            t.addCell(cell(datum, bg, Element.ALIGN_LEFT));
            t.addCell(cell(nr, bg, Element.ALIGN_LEFT));
            t.addCell(cell(kat, bg, Element.ALIGN_LEFT));
            t.addCell(cell(lief, bg, Element.ALIGN_LEFT));
            t.addCell(cell(konto, bg, Element.ALIGN_LEFT));
            t.addCell(cell(desc, bg, Element.ALIGN_LEFT));
            t.addCell(cell(mwst, bg, Element.ALIGN_RIGHT));
            t.addCell(cell(netto, bg, Element.ALIGN_RIGHT));
            t.addCell(cell(brutto, bg, Element.ALIGN_RIGHT));
            alt = !alt;

            sumNetto = sumNetto.add(nullSafe(b.getBetragNetto()));
            sumBrutto = sumBrutto.add(nullSafe(b.getBetragBrutto()));
        }

        // Summenzeile
        Font sumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
        PdfPCell label = new PdfPCell(new Phrase("Summe", sumFont));
        label.setColspan(7);
        label.setHorizontalAlignment(Element.ALIGN_RIGHT);
        label.setBackgroundColor(SUM_BG);
        label.setPaddingTop(8f); label.setPaddingBottom(8f); label.setPaddingRight(10f);
        label.setBorder(Rectangle.NO_BORDER);
        t.addCell(label);

        PdfPCell sn = new PdfPCell(new Phrase(formatEuro(sumNetto), sumFont));
        sn.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sn.setBackgroundColor(SUM_BG);
        sn.setPaddingTop(8f); sn.setPaddingBottom(8f); sn.setPaddingRight(6f); sn.setPaddingLeft(6f);
        sn.setBorder(Rectangle.NO_BORDER);
        t.addCell(sn);

        PdfPCell sb = new PdfPCell(new Phrase(formatEuro(sumBrutto), sumFont));
        sb.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sb.setBackgroundColor(SUM_BG);
        sb.setPaddingTop(8f); sb.setPaddingBottom(8f); sb.setPaddingRight(6f); sb.setPaddingLeft(6f);
        sb.setBorder(Rectangle.NO_BORDER);
        t.addCell(sb);

        doc.add(t);
    }

    private void addFooter(Document doc) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, FOOTER_GREY);
        Paragraph footer = new Paragraph(
                "Dieses Dokument wurde maschinell erstellt und enthält nur validierte Belege. "
                + "Die zugehörigen Belegfotos liegen im selben Ordner.",
                footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(16f);
        doc.add(footer);
    }

    // ===================== Cells & Helpers =====================

    private void addHeader(PdfPTable t, String... headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            c.setBackgroundColor(HEADER_BG);
            c.setPaddingTop(7f); c.setPaddingBottom(7f);
            c.setPaddingLeft(6f); c.setPaddingRight(6f);
            c.setBorder(Rectangle.NO_BORDER);
            t.addCell(c);
        }
    }

    private PdfPCell cell(String text, Color bg, int alignment) {
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_CELL);
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, cellFont));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(alignment);
        c.setPaddingTop(6f); c.setPaddingBottom(6f);
        c.setPaddingLeft(6f); c.setPaddingRight(6f);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.5f);
        return c;
    }

    private PdfPCell kpiCell(String label, BigDecimal value) {
        return kpiCell(label, value, true);
    }

    private PdfPCell kpiCell(String label, BigDecimal value, boolean asEuro) {
        Font lbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, TEXT_MUTED);
        Font val = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, KPI_ACCENT);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label.toUpperCase(java.util.Locale.GERMAN) + "\n", lbl));
        p.add(new Chunk(asEuro ? formatEuro(value) + " €" : value.toPlainString(), val));

        PdfPCell c = new PdfPCell(p);
        c.setBackgroundColor(KPI_BG);
        c.setPaddingTop(8f); c.setPaddingBottom(8f);
        c.setPaddingLeft(10f); c.setPaddingRight(10f);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.5f);
        return c;
    }

    private void addSumRow(PdfPTable t, String label, BigDecimal value) {
        Font sumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
        PdfPCell l = new PdfPCell(new Phrase(label, sumFont));
        l.setColspan(4);
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setBackgroundColor(SUM_BG);
        l.setPaddingTop(7f); l.setPaddingBottom(7f); l.setPaddingRight(10f);
        l.setBorder(Rectangle.NO_BORDER);
        t.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(formatEuro(value) + " €", sumFont));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setBackgroundColor(SUM_BG);
        v.setPaddingTop(7f); v.setPaddingBottom(7f); v.setPaddingRight(6f); v.setPaddingLeft(6f);
        v.setBorder(Rectangle.NO_BORDER);
        t.addCell(v);
    }

    private void addTotalRow(PdfPTable t, String label, BigDecimal value) {
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, KPI_ACCENT);
        PdfPCell l = new PdfPCell(new Phrase(label, totalFont));
        l.setColspan(4);
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setBackgroundColor(KPI_BG);
        l.setPaddingTop(10f); l.setPaddingBottom(10f); l.setPaddingRight(10f);
        l.setBorder(Rectangle.NO_BORDER);
        t.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(formatEuro(value) + " €", totalFont));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setBackgroundColor(KPI_BG);
        v.setPaddingTop(10f); v.setPaddingBottom(10f); v.setPaddingRight(6f); v.setPaddingLeft(6f);
        v.setBorder(Rectangle.NO_BORDER);
        t.addCell(v);
    }

    private BigDecimal sumKategorie(List<Beleg> belege, BelegKategorie kategorie) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Beleg b : belege) {
            if (b.getBelegKategorie() == kategorie) {
                sum = sum.add(nullSafe(b.getBetragBrutto()));
            }
        }
        return sum;
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String formatEuro(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
        // Deutsche Formatierung: 1.234,56
        return String.format(java.util.Locale.GERMAN, "%,.2f", x);
    }

    private String typLabel(SachkontoTyp typ) {
        if (typ == null) return "–";
        return switch (typ) {
            case AUFWAND -> "Aufwand";
            case ERTRAG  -> "Ertrag";
            case PRIVAT  -> "Privat";
            case NEUTRAL -> "Neutral";
        };
    }

    private String kategorieLabel(BelegKategorie k) {
        if (k == null) return "–";
        return switch (k) {
            case UNZUGEORDNET    -> "Unzugeordnet";
            case KASSE_EINNAHME  -> "Kasse · Einnahme";
            case KASSE_AUSGABE   -> "Kasse · Ausgabe";
            case PRIVATENTNAHME  -> "Privatentnahme";
            case PRIVATEINLAGE   -> "Privateinlage";
            case BANK            -> "Bank";
            case KREDITKARTE     -> "Kreditkarte";
            case SONSTIGER_BELEG -> "Sonstiger Beleg";
        };
    }

    private String monatLabel(YearMonth ym) {
        String[] monate = { "Januar", "Februar", "März", "April", "Mai", "Juni",
                "Juli", "August", "September", "Oktober", "November", "Dezember" };
        return monate[ym.getMonthValue() - 1] + " " + ym.getYear();
    }
}
