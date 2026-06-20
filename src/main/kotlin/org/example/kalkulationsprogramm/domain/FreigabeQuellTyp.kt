package org.example.kalkulationsprogramm.domain;

/**
 * Quelle eines Geschäftsdokuments, für das eine digitale Freigabe ausgestellt wurde.
 * Wird in der Tabelle dokument_freigabe gespeichert, da Anfrage- und Projekt-Dokumente
 * in unterschiedlichen Tabellen liegen und keine gemeinsame Foreign Key-Basis haben.
 */
enum class FreigabeQuellTyp {
    ANFRAGE,
    PROJEKT,
    AUSGANGS_DOKUMENT
}
