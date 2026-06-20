package org.example.kalkulationsprogramm.domain.miete

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
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "mietpartei")
open class Mietpartei {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mietobjekt_id", nullable = false)
    open var mietobjekt: Mietobjekt? = null

    @Column(nullable = false)
    open var name: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var rolle: MietparteiRolle? = null

    open var email: String? = null

    open var telefon: String? = null

    @Column(name = "monatlicher_vorschuss", precision = 19, scale = 2)
    open var monatlicherVorschuss: BigDecimal? = null

}
