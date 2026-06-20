package org.example.kalkulationsprogramm.domain;

/**
 * Buchhalterischer Typ eines Sachkontos.
 *
 * AUFWAND  – Kosten (Tankkosten, Bürobedarf, Material)
 * ERTRAG   – Einnahmen (Erlöse 19%, Erlöse 7%)
 * PRIVAT   – Privatentnahme / Privateinlage (eigenkapitalwirksam)
 * NEUTRAL  – Geldbewegungen ohne GuV-Wirkung (Bank-Kassen-Umbuchung,
 *            durchlaufende Posten). Wird in Auswertungen nicht summiert.
 */
enum class SachkontoTyp {
    AUFWAND,
    ERTRAG,
    PRIVAT,
    NEUTRAL
}
