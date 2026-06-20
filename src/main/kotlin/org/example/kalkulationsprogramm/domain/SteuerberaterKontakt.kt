package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "steuerberater_kontakt")
open class SteuerberaterKontakt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false)
    open var name: String? = null

    @Column(nullable = false)
    open var email: String? = null

    open var telefon: String? = null

    open var ansprechpartner: String? = null

    @Column(nullable = false)
    open var autoProcessEmails: Boolean? = true

    @Column(nullable = false)
    open var aktiv: Boolean? = true

    @Column(length = 500)
    open var notizen: String? = null

    open var gueltigAb: LocalDate? = null

    open var gueltigBis: LocalDate? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "steuerberater_kontakt_emails")
    @Column(name = "email")
    open var weitereEmails: MutableSet<String> = mutableSetOf()

    @OneToMany(mappedBy = "steuerberater", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true, fetch = FetchType.LAZY)
    open var ansprechpartnerListe: MutableList<SteuerberaterAnsprechpartner> = mutableListOf()

}
