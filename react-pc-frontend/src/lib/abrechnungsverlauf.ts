import type { AbrechnungsverlaufDto } from '../types';

/**
 * "Einfache Rechnung" darf nur dann angeboten werden, wenn auf dem Basisdokument
 * noch keine *aktive* Folgerechnung existiert. Stornierte Folgerechnungen zählen
 * nicht, weil ihr Betrag im Restbetrag bereits wieder freigegeben wurde — sonst
 * gibt es nach einer Storno-Aktion keinen Weg zurück zur einfachen Rechnung
 * (Bug: nur Teilrechnung/Abschlagsrechnung wären sichtbar).
 */
export function canCreateEinfacheRechnung(
    abrechnungsverlauf: Pick<AbrechnungsverlaufDto, 'positionen'> | null | undefined,
): boolean {
    if (!abrechnungsverlauf) return false;
    return abrechnungsverlauf.positionen.every(p => p.storniert);
}
