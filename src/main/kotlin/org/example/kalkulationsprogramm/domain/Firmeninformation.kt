package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "firmeninformation")
open class Firmeninformation {
    @Id
    open var id: Long? = 1L

    @Column(nullable = false)
    open var firmenname: String? = null

    open var strasse: String? = null

    open var plz: String? = null

    open var ort: String? = null

    open var telefon: String? = null

    open var fax: String? = null

    open var email: String? = null

    open var website: String? = null

    // Steuerliche Angaben
    open var steuernummer: String? = null

    open var ustIdNr: String? = null

    open var handelsregister: String? = null

    open var handelsregisterNummer: String? = null

    // Bankverbindung
    open var bankName: String? = null

    open var iban: String? = null

    open var bic: String? = null

    // Logo als Datei-Referenz
    open var logoDateiname: String? = null

    // Geschäftsführer / Inhaber
    open var geschaeftsfuehrer: String? = null

    // Zusätzliche Felder für Dokumente
    @Column(length = 1000)
    open var fusszeileText: String? = null

    // URL zur Google-Bewertungsseite des Betriebs.
    // Wird in E-Mail-Vorlagen über den Platzhalter {{REVIEW_LINK}} als klickbarer Link eingesetzt.
    @Column(name = "google_bewertungs_link", length = 500)
    open var googleBewertungsLink: String? = null

    // --- Automatisches Mahnverfahren ---
    // Opt-In: erst aktivieren, wenn die Tage-Schwellen vom Inhaber bestaetigt sind.
    @Column(name = "mahnverfahren_aktiv", nullable = false)
    open var mahnverfahrenAktiv: Boolean = false

    // Tage nach Faelligkeitsdatum der Rechnung, bis die Zahlungserinnerung
    // automatisch ausgeloest wird.
    @Column(name = "tage_bis_zahlungserinnerung", nullable = false)
    open var tageBisZahlungserinnerung: Int = 7

    // Abstand in Tagen NACH dem Versand der Zahlungserinnerung, bis die
    // 1. Mahnung folgt (nicht: Tage nach Faelligkeit der Rechnung).
    @Column(name = "tage_bis_erste_mahnung", nullable = false)
    open var tageBisErsteMahnung: Int = 7

    // Abstand in Tagen NACH dem Versand der 1. Mahnung, bis die 2. Mahnung
    // folgt (nicht: Tage nach Faelligkeit der Rechnung).
    @Column(name = "tage_bis_zweite_mahnung", nullable = false)
    open var tageBisZweiteMahnung: Int = 7

    // Neues Zahlungsziel, das jede ausgeloeste Mahnung dem Kunden setzt.
    @Column(name = "mahnverfahren_neues_zahlungsziel_tage", nullable = false)
    open var mahnverfahrenNeuesZahlungszielTage: Int = 7

    // Gewerk der Firma - liefert den Default-BG-Satz (Unfallversicherung).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gewerk_id")
    open var gewerk: Gewerk? = null

    // Tatsaechlicher BG-Satz aus dem Beitragsbescheid. Wenn NULL, gilt der
    // Default-Satz aus dem zugeordneten Gewerk.
    open fun isMahnverfahrenAktiv(): Boolean = mahnverfahrenAktiv
    @Column(name = "bg_satz_override", precision = 5, scale = 2)
    open var bgSatzOverride: BigDecimal? = null

}
