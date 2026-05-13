package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Verteilung des betrieblichen Beleg-Anteils auf mehrere Kostenstellen.
 *
 * Bisher kannte der Beleg genau eine Kostenstelle (Beleg.kostenstelle). Bei
 * Bar-Einkaeufen, die sich auf z.B. Werkstatt (60%) und Gemeinkosten (40%)
 * aufteilen, wird stattdessen pro Beleg eine Liste BelegKostenstellenAnteil
 * gepflegt. Wenn die Liste leer ist, gilt weiterhin die Einzel-Kostenstelle
 * (rueckwaertskompatibel).
 *
 * Unterstuetzt zusaetzlich Kostenstreckung: z.B. eine Zertifizierungsgebuehr
 * von 4.000 EUR mit streckungJahre=4 wird in den Auswertungen 4 Jahre lang
 * mit jeweils 1.000 EUR/Jahr verrechnet — analog zu
 * {@link LieferantDokumentProjektAnteil}.
 */
@Getter
@Setter
@Entity
@Table(name = "beleg_kostenstellen_anteil")
public class BelegKostenstellenAnteil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beleg_id", nullable = false)
    private Beleg beleg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kostenstelle_id", nullable = false)
    private Kostenstelle kostenstelle;

    /**
     * Prozentuale Zuordnung (0-100). Wird verwendet, wenn absoluterBetrag null
     * ist. Genau eines der beiden Felder muss gesetzt sein.
     */
    private Integer prozent;

    /**
     * Absoluter Betrag in EUR. Wenn gesetzt, hat er Vorrang vor prozent.
     */
    @Column(name = "absoluter_betrag", precision = 15, scale = 2)
    private BigDecimal absoluterBetrag;

    /**
     * Automatisch berechnet beim Speichern.
     */
    @Column(name = "berechneter_betrag", precision = 15, scale = 2)
    private BigDecimal berechneterBetrag;

    @Column(length = 255)
    private String beschreibung;

    @Column(name = "zugeordnet_am")
    private LocalDateTime zugeordnetAm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zugeordnet_von_user_id")
    private FrontendUserProfile zugeordnetVon;

    @Column(name = "streckung_jahre", nullable = false)
    private Integer streckungJahre = 1;

    @Column(name = "streckung_start_jahr")
    private Integer streckungStartJahr;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (zugeordnetAm == null) {
            zugeordnetAm = LocalDateTime.now();
        }
        if (streckungJahre == null) {
            streckungJahre = 1;
        }
        // Genau EINES der beiden Felder muss gesetzt sein — andernfalls hat der
        // Split keinen ableitbaren Betrag und ist ein stiller Datenmuell-Eintrag.
        if (prozent == null && absoluterBetrag == null) {
            throw new IllegalStateException(
                    "BelegKostenstellenAnteil: entweder prozent ODER absoluterBetrag muss gesetzt sein");
        }
        if (prozent != null && absoluterBetrag != null) {
            throw new IllegalStateException(
                    "BelegKostenstellenAnteil: nur EINES von prozent oder absoluterBetrag darf gesetzt sein");
        }
        if (prozent != null && (prozent < 0 || prozent > 100)) {
            throw new IllegalStateException(
                    "BelegKostenstellenAnteil: prozent muss zwischen 0 und 100 liegen");
        }
    }

    /**
     * Kostenstellen werden netto verrechnet — der Vorsteuerabzug landet beim
     * Finanzamt, nicht in den Gemeinkosten. Wenn netto null ist, faellt die
     * Berechnung auf brutto zurueck.
     */
    public void berechneAnteil(BigDecimal nettoBetrag, BigDecimal bruttoBetrag) {
        if (absoluterBetrag != null) {
            this.berechneterBetrag = absoluterBetrag;
            return;
        }
        if (prozent == null) {
            return;
        }
        BigDecimal basis = nettoBetrag != null ? nettoBetrag : bruttoBetrag;
        if (basis == null) {
            return;
        }
        this.berechneterBetrag = basis
                .multiply(BigDecimal.valueOf(prozent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getJahresanteil() {
        if (berechneterBetrag == null) {
            return BigDecimal.ZERO;
        }
        if (streckungJahre == null || streckungJahre <= 1) {
            return berechneterBetrag;
        }
        return berechneterBetrag.divide(
                BigDecimal.valueOf(streckungJahre), 2, RoundingMode.HALF_UP);
    }

    @Transient
    public boolean isStreckungAktivFuerJahr(int jahr) {
        if (streckungStartJahr == null || streckungJahre == null || streckungJahre <= 1) {
            return streckungStartJahr == null || streckungStartJahr == jahr;
        }
        return jahr >= streckungStartJahr && jahr < streckungStartJahr + streckungJahre;
    }
}
