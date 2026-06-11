/**
 * Vitest-Suite fuer helpers – Schwerpunkt: extractBoldFromHtml /
 * extractFontSizeFromHtml.
 *
 * Hintergrund (Regressions-Bug, 2026-05-21):
 *  Im Backend (RechnungPdfService#parseHtmlToElements) wird der DTO-Wert
 *  `fett` als `defaultBold` benutzt. Steht der auf true, wird JEDER
 *  Text-Chunk ausserhalb von <strong>/<span>-Tags fett gerendert – also
 *  praktisch die ganze Leistung.
 *
 *  `extractBoldFromHtml` liefert aber bereits dann true, wenn auch nur EIN
 *  Wort fett markiert ist. Daraus folgt:
 *   - Diese Funktion darf NICHT als Block-Default fuer Tiptap-HTML benutzt
 *     werden (TEXT- und SERVICE-Bloecke senden `fett: false` ans Backend,
 *     siehe index.tsx -> contentBlocks-Mapping).
 *
 * DSGVO: ausschliesslich Dummy-Daten.
 */
import { describe, it, expect } from 'vitest';
import {
    extractBoldFromHtml,
    extractFontSizeFromHtml,
    zahlungszielPlaceholderToChipHtml,
    chipHtmlToZahlungszielPlaceholder,
    ZAHLUNGSZIEL_PLACEHOLDER,
    ZAHLUNGSZIEL_TAGE_PLACEHOLDER,
} from './helpers';

describe('extractBoldFromHtml', () => {
    it('liefert false fuer leeres oder undefiniertes HTML', () => {
        expect(extractBoldFromHtml('')).toBe(false);
        expect(extractBoldFromHtml(undefined as unknown as string)).toBe(false);
    });

    it('liefert false fuer reinen Text ohne Markup', () => {
        expect(extractBoldFromHtml('<p>Maler streichen Wand</p>')).toBe(false);
    });

    it('liefert true wenn EIN einzelnes Wort per <strong> fett markiert ist (Bug-Regression)', () => {
        // Genau dieses HTML kommt aus TiptapEditor, wenn der User nur ein
        // einzelnes Wort fett setzt. Vor dem Fix wurde dieser true-Wert
        // als block.fett ans Backend geschickt und liess die GANZE Leistung
        // fett werden.
        const html = '<p>Maler streicht <strong>weisse</strong> Wand</p>';
        expect(extractBoldFromHtml(html)).toBe(true);
    });

    it('erkennt auch <b>-Tag und font-weight im Style', () => {
        expect(extractBoldFromHtml('<p>Text <b>fett</b> mehr</p>')).toBe(true);
        expect(extractBoldFromHtml('<p><span style="font-weight: 700">fett</span></p>')).toBe(true);
    });
});

describe('extractFontSizeFromHtml', () => {
    it('liefert undefined wenn keine font-size im HTML steht', () => {
        expect(extractFontSizeFromHtml('<p>Plain Text Mustermann</p>')).toBeUndefined();
    });

    it('liefert die dominante (haeufigste) Groesse zurueck', () => {
        const html =
            '<p><span style="font-size: 12pt">Eins</span> ' +
            '<span style="font-size: 14pt">Zwei</span> ' +
            '<span style="font-size: 14pt">Drei</span></p>';
        expect(extractFontSizeFromHtml(html)).toBe(14);
    });

    it('clampt Werte ausserhalb 10-20pt', () => {
        expect(extractFontSizeFromHtml('<span style="font-size: 6pt">x</span>')).toBe(10);
        expect(extractFontSizeFromHtml('<span style="font-size: 48pt">x</span>')).toBe(20);
    });
});

/**
 * Zahlungsziel-Chips (Regressions-Bug, 2026-06-11):
 *  Das Zahlungsziel im Textbaustein stand als eingefrorener Klartext
 *  ("8 Tage" / "17.06.2026") im Editor statt als geschuetzter Chip.
 *  Beide Platzhalter ({{ZAHLUNGSZIEL}} = Faelligkeitsdatum,
 *  {{ZAHLUNGSZIEL_TAGE}} = Anzahl Tage) muessen verlustfrei in Chip-Spans
 *  und wieder zurueck wandelbar sein — sonst friert der Wert beim Speichern ein.
 */
describe('zahlungszielPlaceholderToChipHtml', () => {
    it('wandelt {{ZAHLUNGSZIEL}} in einen Datum-Chip', () => {
        const html = '<p>Zahlbar bis: {{ZAHLUNGSZIEL}}</p>';
        expect(zahlungszielPlaceholderToChipHtml(html, '17.06.2026', '8'))
            .toBe('<p>Zahlbar bis: <span data-zahlungsziel-chip="datum">17.06.2026</span></p>');
    });

    it('wandelt {{ZAHLUNGSZIEL_TAGE}} in einen Tage-Chip (Bug-Regression)', () => {
        const html = '<p>Zahlbar innerhalb von {{ZAHLUNGSZIEL_TAGE}} Tagen nach Rechnungsdatum.</p>';
        expect(zahlungszielPlaceholderToChipHtml(html, '17.06.2026', '8'))
            .toBe('<p>Zahlbar innerhalb von <span data-zahlungsziel-chip="tage">8</span> Tagen nach Rechnungsdatum.</p>');
    });

    it('verwechselt die beiden Platzhalter nicht, wenn beide vorkommen', () => {
        const html = '<p>{{ZAHLUNGSZIEL_TAGE}} Tage, faellig {{ZAHLUNGSZIEL}}</p>';
        const result = zahlungszielPlaceholderToChipHtml(html, '17.06.2026', '8');
        expect(result).toContain('<span data-zahlungsziel-chip="tage">8</span>');
        expect(result).toContain('<span data-zahlungsziel-chip="datum">17.06.2026</span>');
    });

    it('toleriert Leerraum und Kleinschreibung im Platzhalter', () => {
        const result = zahlungszielPlaceholderToChipHtml('<p>{{ zahlungsziel }}</p>', '17.06.2026', '8');
        expect(result).toBe('<p><span data-zahlungsziel-chip="datum">17.06.2026</span></p>');
    });
});

describe('chipHtmlToZahlungszielPlaceholder', () => {
    it('serialisiert den Datum-Chip zurueck zu {{ZAHLUNGSZIEL}}', () => {
        const html = '<p>Zahlbar bis: <span data-zahlungsziel-chip="datum">17.06.2026</span></p>';
        expect(chipHtmlToZahlungszielPlaceholder(html))
            .toBe(`<p>Zahlbar bis: ${ZAHLUNGSZIEL_PLACEHOLDER}</p>`);
    });

    it('serialisiert den Tage-Chip zurueck zu {{ZAHLUNGSZIEL_TAGE}} (Bug-Regression)', () => {
        const html = '<p>von <span data-zahlungsziel-chip="tage">8</span> Tagen</p>';
        expect(chipHtmlToZahlungszielPlaceholder(html))
            .toBe(`<p>von ${ZAHLUNGSZIEL_TAGE_PLACEHOLDER} Tagen</p>`);
    });

    it('interpretiert Legacy-Chips (data-zahlungsziel-chip="true") als Datum', () => {
        const html = '<p><span data-zahlungsziel-chip="true">17.06.2026</span></p>';
        expect(chipHtmlToZahlungszielPlaceholder(html))
            .toBe(`<p>${ZAHLUNGSZIEL_PLACEHOLDER}</p>`);
    });

    it('ist verlustfrei im Round-Trip Platzhalter -> Chip -> Platzhalter', () => {
        const original = '<p>Zahlbar innerhalb von {{ZAHLUNGSZIEL_TAGE}} Tagen, faellig am {{ZAHLUNGSZIEL}}.</p>';
        const chipped = zahlungszielPlaceholderToChipHtml(original, '17.06.2026', '8');
        expect(chipHtmlToZahlungszielPlaceholder(chipped)).toBe(original);
    });

    it('laesst HTML ohne Chips unveraendert', () => {
        const html = '<p>Mit freundlichen Gruessen, Max Mustermann</p>';
        expect(chipHtmlToZahlungszielPlaceholder(html)).toBe(html);
    });
});
