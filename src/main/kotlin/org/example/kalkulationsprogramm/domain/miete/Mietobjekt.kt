package org.example.kalkulationsprogramm.domain.miete

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "mietobjekt")
open class Mietobjekt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false, unique = true)
    open var name: String? = null

    open var strasse: String? = null

    open var plz: String? = null

    open var ort: String? = null

    @OneToMany(mappedBy = "mietobjekt", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var mietparteien: MutableList<Mietpartei> = mutableListOf()

    @OneToMany(mappedBy = "mietobjekt", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var raeume: MutableList<Raum> = mutableListOf()

    @OneToMany(mappedBy = "mietobjekt", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var kostenstellen: MutableList<Kostenstelle> = mutableListOf()

    @OneToMany(mappedBy = "mietobjekt", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var verteilungsschluessel: MutableList<Verteilungsschluessel> = mutableListOf()

}
