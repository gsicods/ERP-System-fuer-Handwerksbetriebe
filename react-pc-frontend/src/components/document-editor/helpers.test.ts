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
import { extractBoldFromHtml, extractFontSizeFromHtml } from './helpers';

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
