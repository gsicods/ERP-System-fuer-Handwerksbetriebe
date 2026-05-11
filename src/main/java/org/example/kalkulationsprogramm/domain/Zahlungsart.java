package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stammdaten-Eintrag fuer Zahlungsarten (Bar, EC-Karte, Ueberweisung, ...).
 *
 * Wird im Belege/Kasse-Editor als Auswahl-Dropdown verwendet. Der String wird
 * in {@code beleg.zahlungsart} weiterhin als Freitext gespeichert (kein FK),
 * damit bestehende Belege unveraendert bleiben und ein Buchhalter im Notfall
 * auch eine nicht gepflegte Bezeichnung erfassen kann.
 */
@Getter
@Setter
@Entity
@Table(name = "zahlungsart")
public class Zahlungsart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String bezeichnung;

    @Column(nullable = false)
    private boolean aktiv = true;

    @Column(nullable = false)
    private int sortierung = 0;
}
