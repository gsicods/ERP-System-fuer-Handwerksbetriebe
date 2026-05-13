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
import org.example.kalkulationsprogramm.repository.BelegRepository;
import org.example.kalkulationsprogramm.repository.FirmeninformationRepository;
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
 *  - Briefkopf mit Logo und Firmenstammdaten
 *  - Titel und Zeitraum
 *  - Kassen-Konto im klassischen T-Konto-Layout (Soll | Haben) mit
 *    Anfangs- und Endsaldo
 *
 * Optisch an {@link ProjektAuswertungPdfService} angelehnt (rose-600 Header,
 * helle Zebra-Streifen, dezente Borders).
 */
@Service
@RequiredArgsConstructor
public class BelegeKasseExportPdfService {

    private final BelegRepository belegRepository;
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
            // Anfangssaldo: Stand am Vortag des Monatsanfangs (alle validierten
            // Bar-Bewegungen vor dem Monat). Steuerberater erwartet kontinuierliche
            // Saldofortschreibung; ohne diesen Wert beginnt das T-Konto irrtuemlich
            // bei 0,00 EUR und stimmt nicht mit dem Vormonats-PDF ueberein.
            BigDecimal anfangssaldo = berechneAnfangssaldo(alle, von);
            addKassenbuchTKonto(doc, imMonat, anfangssaldo);
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

    /**
     * Anfangssaldo am Vortag des Monatsanfangs. Summiert ueber alle validierten
     * Bar-Bewegungen vor dem Stichtag — Eingaenge addiert, Ausgaenge subtrahiert.
     * Damit beginnt das T-Konto nicht bei 0, sondern setzt den Endsaldo des
     * Vormonats lueckenlos fort.
     */
    private BigDecimal berechneAnfangssaldo(List<Beleg> alleValidiert, LocalDate monatsAnfang) {
        BigDecimal saldo = BigDecimal.ZERO;
        for (Beleg b : alleValidiert) {
            if (b.getBelegDatum() == null || !b.getBelegDatum().isBefore(monatsAnfang)) continue;
            BelegKategorie k = b.getBelegKategorie();
            if (k == null || !k.istKassenBewegung()) continue;
            BigDecimal brutto = nullSafe(b.getBetragBrutto());
            saldo = k.istAusgang() ? saldo.subtract(brutto) : saldo.add(brutto);
        }
        return saldo;
    }

    /**
     * Kassen-Konto im klassischen T-Konto-Layout (Steuerberater-Standard):
     * Linke Spalte = Soll (Eingaenge: KASSE_EINNAHME + PRIVATEINLAGE),
     * rechte Spalte = Haben (Ausgaenge: KASSE_AUSGABE + PRIVATENTNAHME).
     * Anfangssaldo wird ueber dem T-Konto angedruckt, Endsaldo darunter.
     * Endsaldo = Anfangssaldo + Summe Soll − Summe Haben.
     */
    private void addKassenbuchTKonto(Document doc, List<Beleg> belege, BigDecimal anfangssaldo)
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK);
        Paragraph section = new Paragraph("Kasse · Bargeldkonto (T-Konto)", sectionFont);
        section.setSpacingBefore(10f);
        doc.add(section);

        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MUTED);
        Paragraph anfang = new Paragraph(
                "Anfangssaldo zu Monatsbeginn: " + formatEuro(anfangssaldo) + " €", infoFont);
        anfang.setSpacingBefore(2f);
        anfang.setSpacingAfter(6f);
        doc.add(anfang);

        List<Beleg> kasse = belege.stream()
                .filter(b -> b.getBelegKategorie() != null
                        && b.getBelegKategorie().istKassenBewegung())
                .sorted(Comparator
                        .comparing(Beleg::getBelegDatum, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Beleg::getId))
                .toList();

        List<Beleg> soll  = kasse.stream()
                .filter(b -> !b.getBelegKategorie().istAusgang()).toList();
        List<Beleg> haben = kasse.stream()
                .filter(b -> b.getBelegKategorie().istAusgang()).toList();

        BigDecimal sumSoll  = summeBrutto(soll);
        BigDecimal sumHaben = summeBrutto(haben);
        BigDecimal endsaldo = anfangssaldo.add(sumSoll).subtract(sumHaben);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4f);

        t.addCell(seitenHeaderCell("SOLL  ·  Eingang"));
        t.addCell(seitenHeaderCell("HABEN  ·  Ausgang"));

        t.addCell(seitenContainer(buildSeitenTabelle(soll)));
        t.addCell(seitenContainer(buildSeitenTabelle(haben)));

        t.addCell(seitenSummeCell("Summe Soll:  " + formatEuro(sumSoll) + " €"));
        t.addCell(seitenSummeCell("Summe Haben: " + formatEuro(sumHaben) + " €"));

        doc.add(t);

        Font endFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, KPI_ACCENT);
        Paragraph end = new Paragraph(
                "Endsaldo (Anfang + Soll − Haben): " + formatEuro(endsaldo) + " €", endFont);
        end.setAlignment(Element.ALIGN_RIGHT);
        end.setSpacingBefore(10f);
        doc.add(end);
    }

    private PdfPTable buildSeitenTabelle(List<Beleg> belege) {
        PdfPTable inner = new PdfPTable(new float[]{ 1.4f, 1.6f, 4.5f, 2f });
        inner.setWidthPercentage(100);

        Font subHdr = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_MUTED);
        String[] heads = { "Datum", "Beleg-Nr.", "Verwendungszweck", "Betrag €" };
        for (String h : heads) {
            PdfPCell c = new PdfPCell(new Phrase(h, subHdr));
            c.setBackgroundColor(SUM_BG);
            c.setPaddingTop(5f); c.setPaddingBottom(5f);
            c.setPaddingLeft(6f); c.setPaddingRight(6f);
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(BORDER);
            c.setBorderWidth(0.5f);
            inner.addCell(c);
        }

        if (belege.isEmpty()) {
            Font muted = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, TEXT_MUTED);
            PdfPCell empty = new PdfPCell(new Phrase("Keine Buchungen", muted));
            empty.setColspan(4);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPaddingTop(10f); empty.setPaddingBottom(10f);
            empty.setBorder(Rectangle.NO_BORDER);
            inner.addCell(empty);
            return inner;
        }

        boolean alt = false;
        for (Beleg b : belege) {
            Color bg = alt ? ROW_ALT : Color.WHITE;
            String datum  = b.getBelegDatum()  != null ? b.getBelegDatum().format(DATE_SHORT) : "–";
            String nr     = b.getBelegNummer() != null ? b.getBelegNummer() : "–";
            String zweck  = b.getBeschreibung() != null && !b.getBeschreibung().isBlank()
                    ? b.getBeschreibung()
                    : kategorieLabel(b.getBelegKategorie());
            String betrag = formatEuro(nullSafe(b.getBetragBrutto()));

            inner.addCell(cell(datum, bg, Element.ALIGN_LEFT));
            inner.addCell(cell(nr,    bg, Element.ALIGN_LEFT));
            inner.addCell(cell(zweck, bg, Element.ALIGN_LEFT));
            inner.addCell(cell(betrag, bg, Element.ALIGN_RIGHT));
            alt = !alt;
        }
        return inner;
    }

    private PdfPCell seitenHeaderCell(String text) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell c = new PdfPCell(new Phrase(text, headerFont));
        c.setBackgroundColor(HEADER_BG);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(8f); c.setPaddingBottom(8f);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell seitenContainer(PdfPTable inner) {
        PdfPCell c = new PdfPCell(inner);
        c.setPadding(0f);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.5f);
        return c;
    }

    private PdfPCell seitenSummeCell(String text) {
        Font sumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_DARK);
        PdfPCell c = new PdfPCell(new Phrase(text, sumFont));
        c.setBackgroundColor(SUM_BG);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPaddingTop(8f); c.setPaddingBottom(8f);
        c.setPaddingLeft(10f); c.setPaddingRight(10f);
        c.setBorderColor(TEXT_DARK);
        c.setBorderWidthTop(1f);
        c.setBorderWidthBottom(0f);
        c.setBorderWidthLeft(0f);
        c.setBorderWidthRight(0f);
        return c;
    }

    private BigDecimal summeBrutto(List<Beleg> belege) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Beleg b : belege) sum = sum.add(nullSafe(b.getBetragBrutto()));
        return sum;
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

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String formatEuro(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
        // Deutsche Formatierung: 1.234,56
        return String.format(java.util.Locale.GERMAN, "%,.2f", x);
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
