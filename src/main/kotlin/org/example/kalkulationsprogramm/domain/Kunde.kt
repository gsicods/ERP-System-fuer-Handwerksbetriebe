package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*

@Entity
open class Kunde {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false, unique = true)
    open var kundennummer: String? = null

    @Column(nullable = false)
    open var name: String? = null

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    open var anrede: Anrede? = null

    open var ansprechspartner: String? = null

    open var strasse: String? = null

    open var plz: String? = null

    open var ort: String? = null

    open var telefon: String? = null

    open var mobiltelefon: String? = null

    open var zahlungsziel: Int? = 8

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "kunden_emails")
    @Column(name = "email", nullable = false, unique = true)
    open var kundenEmails: MutableList<String> = mutableListOf()

    @OneToMany(mappedBy = "kundenId", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var projekts: MutableList<Projekt>? = mutableListOf()

}
