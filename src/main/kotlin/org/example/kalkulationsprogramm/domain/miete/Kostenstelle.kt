package org.example.kalkulationsprogramm.domain.miete

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "MieteKostenstelle")
@Table(name = "miete_kostenstelle")
open class Kostenstelle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mietobjekt_id", nullable = false)
    open var mietobjekt: Mietobjekt? = null

    @Column(nullable = false)
    open var name: String? = null

    open var beschreibung: String? = null

    @Column(nullable = false)
    open var umlagefaehig: Boolean = true

    open fun isUmlagefaehig(): Boolean = umlagefaehig

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_schluessel_id")
    open var standardSchluessel: Verteilungsschluessel? = null

    @OneToMany(mappedBy = "kostenstelle")
    open var kostenpositionen: MutableList<Kostenposition> = mutableListOf()

}
