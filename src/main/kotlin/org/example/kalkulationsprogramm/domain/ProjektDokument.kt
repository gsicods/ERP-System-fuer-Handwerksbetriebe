package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
open class ProjektDokument : Dokument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null

    @Column(nullable = false)
    override var originalDateiname: String? = null

    @Column(nullable = false, unique = true)
    override var gespeicherterDateiname: String? = null

    override var dateityp: String? = null

    override var dateigroesse: Long? = null

    override var uploadDatum: LocalDate? = null

    override var emailVersandDatum: LocalDate? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    override var dokumentGruppe: DokumentGruppe? = DokumentGruppe.DIVERSE_DOKUMENTE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Projekt")
    open var projekt: Projekt? = null

    // Tracks which employee uploaded this document (nullable for legacy data)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    open var uploadedBy: Mitarbeiter? = null

    // Optional: Links document to a supplier (e.g., for invoices, delivery notes)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("projekte", "emails", "bestellungen")
    open var lieferant: Lieferanten? = null

}
