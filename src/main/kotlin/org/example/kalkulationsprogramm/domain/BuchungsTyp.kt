package org.example.kalkulationsprogramm.domain;

/**
 * Typ einer Zeitbuchung.
 * ARBEIT = normale Arbeitsstunden auf einem Projekt
 * PAUSE = Pausenzeit (wird nicht zur Arbeitszeit gezählt)
 */
enum class BuchungsTyp {
    ARBEIT,
    PAUSE
}
