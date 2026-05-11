package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Sachkonto für die Beleg-Erfassung (Buchhaltung).
 *
 * Klassische Aufwand/Ertrag/Privat-Konten zur Auswertung "Wieviel haben wir
 * dieses Jahr für X ausgegeben?". Nummern sind an SKR03 angelehnt, aber nicht
 * zwingend — Handwerker können auch komplett ohne Nummer arbeiten.
 */
@Getter
@Setter
@Entity
@Table(name = "sachkonto")
public class Sachkonto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String nummer;

    @Column(nullable = false, length = 120, unique = true)
    private String bezeichnung;

    @Enumerated(EnumType.STRING)
    @Column(name = "konto_typ", nullable = false, length = 20)
    private SachkontoTyp kontoTyp;

    @Column(length = 500)
    private String beschreibung;

    @Column(nullable = false)
    private boolean aktiv = true;

    @Column(nullable = false)
    private int sortierung = 0;
}
