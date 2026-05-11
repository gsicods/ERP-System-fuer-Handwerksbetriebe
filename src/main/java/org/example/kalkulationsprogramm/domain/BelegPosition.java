package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Eine einzelne Position auf einem Beleg (KI-extrahiert aus dem Scan).
 *
 * Wird nur fuer Belege mit {@link BelegAufteilungsModus#TEILWEISE} angelegt.
 * Der Mitarbeiter setzt am Handy per Checkbox {@link #istFuerFirma} pro
 * Position; BelegSplitService rechnet daraus die Firma-Summen am Beleg neu.
 *
 * mwstSatz wird pro Position gespeichert, damit gemischte MwSt-Saetze auf
 * einem Bon (z.B. 7% Lebensmittel + 19% Werkzeug) korrekt aufgeteilt werden.
 */
@Getter
@Setter
@Entity
@Table(name = "beleg_position")
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "sortierung", "beschreibung", "istFuerFirma"})
public class BelegPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beleg_id", nullable = false)
    private Beleg beleg;

    /** Reihenfolge auf dem Original-Bon (KI uebernimmt die visuelle Sortierung). */
    @Column(nullable = false)
    private int sortierung;

    @Column(nullable = false, length = 500)
    private String beschreibung;

    @Column(precision = 15, scale = 3)
    private BigDecimal menge;

    @Column(length = 20)
    private String einheit;

    @Column(precision = 15, scale = 4)
    private BigDecimal einzelpreis;

    @Column(name = "betrag_netto", precision = 15, scale = 2)
    private BigDecimal betragNetto;

    @Column(name = "betrag_brutto", precision = 15, scale = 2)
    private BigDecimal betragBrutto;

    @Column(name = "mwst_satz", precision = 5, scale = 2)
    private BigDecimal mwstSatz;

    @Column(name = "ist_fuer_firma", nullable = false)
    private boolean istFuerFirma = false;

    @Column(name = "erstellt_am", nullable = false)
    private LocalDateTime erstelltAm;

    @PrePersist
    void onCreate() {
        if (erstelltAm == null) {
            erstelltAm = LocalDateTime.now();
        }
    }
}
