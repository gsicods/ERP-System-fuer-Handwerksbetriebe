package org.example.kalkulationsprogramm.domain;

/**
 * Typ der Zuordnung einer Email.
 * Exklusiv: Eine Email kann nur zu EINER Entität gehören.
 */
enum class EmailZuordnungTyp {
    PROJEKT,
    ANFRAGE,
    LIEFERANT,
    STEUERBERATER,
    KEINE
}
