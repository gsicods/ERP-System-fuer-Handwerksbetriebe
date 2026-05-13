import { describe, it, expect } from 'vitest';
import { canCreateEinfacheRechnung } from './abrechnungsverlauf';
import type { AbrechnungsverlaufDto, AbrechnungspositionDto } from '../types';

const basisVerlauf: Omit<AbrechnungsverlaufDto, 'positionen'> = {
    basisdokumentId: 1,
    basisdokumentNummer: 'AB-2026-0001',
    basisdokumentTyp: 'AUFTRAGSBESTAETIGUNG',
    basisdokumentBetragNetto: 1000,
    bereitsAbgerechnet: 0,
    restbetrag: 1000,
};

const position = (overrides: Partial<AbrechnungspositionDto>): AbrechnungspositionDto => ({
    id: 100,
    dokumentNummer: 'RE-2026-0001',
    typ: 'RECHNUNG',
    datum: '2026-05-01',
    betragNetto: 1000,
    storniert: false,
    ...overrides,
});

describe('canCreateEinfacheRechnung', () => {
    it('liefert false bei null/undefined Verlauf', () => {
        expect(canCreateEinfacheRechnung(null)).toBe(false);
        expect(canCreateEinfacheRechnung(undefined)).toBe(false);
    });

    it('liefert true, wenn noch keine Folgerechnung existiert (Happy Path)', () => {
        const verlauf: AbrechnungsverlaufDto = { ...basisVerlauf, positionen: [] };
        expect(canCreateEinfacheRechnung(verlauf)).toBe(true);
    });

    it('liefert true, wenn die einzige einfache Rechnung storniert wurde (Bug-Szenario)', () => {
        // Regression: Vorher wurde nur length===0 geprueft, daher war "Einfache Rechnung"
        // nach einer Storno-Aktion nicht mehr waehlbar, obwohl der Restbetrag wieder voll
        // verfuegbar ist.
        const verlauf: AbrechnungsverlaufDto = {
            ...basisVerlauf,
            positionen: [position({ storniert: true })],
        };
        expect(canCreateEinfacheRechnung(verlauf)).toBe(true);
    });

    it('liefert true, wenn alle bisherigen Rechnungen (egal welcher Typ) storniert sind', () => {
        const verlauf: AbrechnungsverlaufDto = {
            ...basisVerlauf,
            positionen: [
                position({ id: 10, typ: 'ABSCHLAGSRECHNUNG', storniert: true }),
                position({ id: 11, typ: 'TEILRECHNUNG', storniert: true }),
            ],
        };
        expect(canCreateEinfacheRechnung(verlauf)).toBe(true);
    });

    it('liefert false, sobald mindestens eine aktive Folgerechnung existiert', () => {
        const verlauf: AbrechnungsverlaufDto = {
            ...basisVerlauf,
            positionen: [
                position({ id: 10, typ: 'ABSCHLAGSRECHNUNG', storniert: true }),
                position({ id: 11, typ: 'ABSCHLAGSRECHNUNG', storniert: false }),
            ],
        };
        expect(canCreateEinfacheRechnung(verlauf)).toBe(false);
    });
});
