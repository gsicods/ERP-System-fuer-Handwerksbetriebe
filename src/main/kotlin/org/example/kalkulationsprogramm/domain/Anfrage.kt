package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "anfrage")
open class Anfrage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    open var bauvorhaben: String? = null

    open var betrag: BigDecimal? = null

    open var emailVersandDatum: LocalDate? = null

    open var anlegedatum: LocalDate? = null

    // Optionales Profilbild für Anfrages-Kacheln
    open var bildUrl: String? = null

    open var projektStrasse: String? = null

    open var projektPlz: String? = null

    open var projektOrt: String? = null

    @Column(length = 1000)
    open var kurzbeschreibung: String? = null

    @OneToMany(mappedBy = "anfrage", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var dokumente: MutableList<AnfrageDokument> = mutableListOf()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "anfrage_kunden_emails")
    @Column(name = "email")
    open var kundenEmails: MutableList<String> = mutableListOf()

    @ManyToOne
    @JoinColumn(name = "kunde_id")
    open var kunde: Kunde? = null

    @ManyToOne
    @JoinColumn(name = "projekt_id")
    open var projekt: Projekt? = null

    @Column(nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    open var createdAt: LocalDateTime? = LocalDateTime.now()

    open var abgeschlossen: Boolean = false

    open fun isAbgeschlossen(): Boolean = abgeschlossen

    @OneToMany(mappedBy = "anfrage", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true, fetch = FetchType.LAZY)
    open var notizen: MutableList<AnfrageNotiz> = mutableListOf()

}
