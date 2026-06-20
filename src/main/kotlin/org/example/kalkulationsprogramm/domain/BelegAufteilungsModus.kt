package org.example.kalkulationsprogramm.domain;

/**
 * Steuert, ob ein gescannter Beleg vollstaendig der Firma zugeordnet wird oder
 * nur in Teilen.
 *
 * VOLLSTAENDIG: Klassischer Fall — der gesamte Beleg (Brutto/Netto/MwSt) wird
 *   1:1 als Geschaeftsausgabe gebucht. {@code betragFirma*}-Felder bleiben null
 *   und der Verbraucher (UI, Verrechnungslohn) liest die Standard-Felder.
 *
 * TEILWEISE: Mischbeleg (z.B. Supermarkt-Bon mit privatem Einkauf + Kaffee
 *   fuer's Buero). Die KI extrahiert beim Scan alle Positionen, der Nutzer
 *   waehlt am Handy per Checkbox die geschaeftlichen aus. BelegSplitService
 *   berechnet daraus {@code betragFirmaNetto / Brutto / Mwst} — diese Werte
 *   sind dann massgeblich fuer die Buchhaltung. Die Original-Summen am Beleg
 *   bleiben erhalten (GoBD: Originalbeleg unveraendert dokumentiert).
 */
enum class BelegAufteilungsModus {
    VOLLSTAENDIG,
    TEILWEISE
}
