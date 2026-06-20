package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*

@Entity
open class Abteilung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false, unique = true)
    open var name: String? = null

    @OneToMany(mappedBy = "abteilung", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var arbeitsgaenge: MutableList<Arbeitsgang> = mutableListOf()

    @Column(nullable = false)
    open var darfRechnungenGenehmigen: Boolean? = false

    @Column(nullable = false)
    open var darfRechnungenSehen: Boolean? = false

    @Column(nullable = false)
    open var darfFreigabeAnnahmePushen: Boolean? = true

    @Column(nullable = false)
    open var darfWebseitenAnfragenPushen: Boolean? = true

}
