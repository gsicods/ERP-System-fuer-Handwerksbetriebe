package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "kalender_eintrag")
open class KalenderEintrag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false)
    open var titel: String? = null

    @Column(length = 2000)
    open var beschreibung: String? = null

    @Column(nullable = false)
    open var datum: LocalDate? = null

    open var startZeit: LocalTime? = null

    open var endeZeit: LocalTime? = null

    @Column(nullable = false)
    open var ganztaegig: Boolean = false

    open fun isGanztaegig(): Boolean = ganztaegig

    open var farbe: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ersteller_id")
    open var ersteller: Mitarbeiter? = null

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "kalender_eintrag_teilnehmer",
        joinColumns = [JoinColumn(name = "kalender_eintrag_id")],
        inverseJoinColumns = [JoinColumn(name = "mitarbeiter_id")]
    )
    open var teilnehmer: MutableSet<Mitarbeiter> = mutableSetOf()

    // Optionale Verknüpfungen
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projekt_id")
    open var projekt: Projekt? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kunde_id")
    open var kunde: Kunde? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_id")
    open var lieferant: Lieferanten? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anfrage_id")
    open var anfrage: Anfrage? = null

    @Column(nullable = false)
    open var erstelltAm: LocalDateTime? = null

    open var aktualisiertAm: LocalDateTime? = null

    @PrePersist
    open fun onCreate() {
        erstelltAm = LocalDateTime.now()
    }

    @PreUpdate
    open fun onUpdate() {
        aktualisiertAm = LocalDateTime.now()
    }

}
