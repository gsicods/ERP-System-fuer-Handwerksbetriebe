package org.example.kalkulationsprogramm.domain.miete

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "verteilungsschluessel")
open class Verteilungsschluessel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mietobjekt_id", nullable = false)
    open var mietobjekt: Mietobjekt? = null

    @Column(nullable = false)
    open var name: String? = null

    open var beschreibung: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var typ: VerteilungsschluesselTyp? = VerteilungsschluesselTyp.PROZENTUAL

    @OneToMany(mappedBy = "verteilungsschluessel", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var eintraege: MutableList<VerteilungsschluesselEintrag>? = mutableListOf()

}
