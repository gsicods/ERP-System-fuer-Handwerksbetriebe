package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Belegerfassung für die Buchhaltung (Kasse, Privatentnahme, Bank etc.).
 *
 * Im Gegensatz zu LieferantDokument kann ein Beleg auch ohne Lieferant
 * existieren (z.B. Bar-Tankquittung, Kassenbon). Der Workflow ist auf schnelles
 * Scannen am Handy + spätere Validierung am PC ausgelegt.
 *
 * HARTE INVARIANTE: gespeicherter_dateiname ist nicht null — es gibt keine
 * Buchhaltungs-Buchung ohne Belegfoto.
 */
@Getter
@Setter
@Entity
@Table(name = "beleg")
public class Beleg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "beleg_kategorie", nullable = false, length = 40)
    private BelegKategorie belegKategorie = BelegKategorie.UNZUGEORDNET;

    /**
     * Vom KI-Analyse-Service erkannter Dokumenttyp. Null = noch nicht analysiert
     * oder kein klassifizierbares Geschaeftsdokument. Wenn RECHNUNG/GUTSCHRIFT
     * und Lieferant gesetzt ist, wird automatisch ein LieferantGeschaeftsdokument
     * erzeugt, damit der Beleg auch in der Rechnungsuebersicht erscheint.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dokument_typ", length = 30)
    private LieferantDokumentTyp dokumentTyp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BelegStatus status = BelegStatus.NEU;

    @Enumerated(EnumType.STRING)
    @Column(name = "ki_analyse_status", nullable = false, length = 20)
    private BelegKiAnalyseStatus kiAnalyseStatus = BelegKiAnalyseStatus.PENDING;

    @Column(name = "beleg_datum")
    private LocalDate belegDatum;

    @Column(name = "beleg_nummer", length = 100)
    private String belegNummer;

    @Column(length = 500)
    private String beschreibung;

    @Column(name = "betrag_netto", precision = 15, scale = 2)
    private BigDecimal betragNetto;

    @Column(name = "betrag_brutto", precision = 15, scale = 2)
    private BigDecimal betragBrutto;

    @Column(name = "mwst_satz", precision = 5, scale = 2)
    private BigDecimal mwstSatz;

    @Column(length = 40)
    private String zahlungsart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_id")
    private Lieferanten lieferant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sachkonto_id")
    private Sachkonto sachkonto;

    @Column(name = "ki_vorgeschlagener_lieferant", length = 255)
    private String kiVorgeschlagenerLieferant;

    @Column(name = "ki_confidence", precision = 3, scale = 2)
    private BigDecimal kiConfidence;

    @Lob
    @Column(name = "ki_extraktion_json", columnDefinition = "LONGTEXT")
    private String kiExtraktionJson;

    @Column(name = "ki_fehler_text", length = 1000)
    private String kiFehlerText;

    @Column(name = "original_dateiname", length = 255)
    private String originalDateiname;

    /**
     * Gespeicherter Dateiname (UUID-Prefix). Nullable, weil Umbuchungen
     * (ist_umbuchung=true) auch ohne Beleg-Datei erfasst werden duerfen.
     * Service erzwingt: NULL nur dann erlaubt wenn istUmbuchung=true.
     */
    @Column(name = "gespeicherter_dateiname", length = 255)
    private String gespeicherterDateiname;

    /**
     * Belegfreie Buchhaltungs-Bewegung (Privatentnahme, Kasse->Bank etc.).
     * Wenn true, darf gespeicherterDateiname NULL bleiben.
     */
    @Column(name = "ist_umbuchung", nullable = false)
    private Boolean istUmbuchung = false;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "upload_datum", nullable = false)
    private LocalDateTime uploadDatum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private Mitarbeiter uploadedBy;

    @Column(name = "validiert_am")
    private LocalDateTime validiertAm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validiert_von_id")
    private Mitarbeiter validiertVon;

    @Column(length = 1000)
    private String notiz;

    @PrePersist
    protected void onCreate() {
        if (uploadDatum == null) {
            uploadDatum = LocalDateTime.now();
        }
    }
}
