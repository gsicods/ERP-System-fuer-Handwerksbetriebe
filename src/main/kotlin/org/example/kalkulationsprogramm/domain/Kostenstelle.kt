package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "FirmaKostenstelle")
@Table(name = "firma_kostenstelle")
open class Kostenstelle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(name = "name", nullable = false, unique = true)
    open var bezeichnung: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var typ: KostenstellenTyp? = null

    @Column(length = 500)
    open var beschreibung: String? = null

    @Column(nullable = false)
    open var istFixkosten: Boolean = false

    @Column(nullable = false)
    open var istInvestition: Boolean = false

    @Column(nullable = false)
    open var aktiv: Boolean = true

    open var sortierung: Int? = 0

    open fun isIstFixkosten(): Boolean = istFixkosten

    open fun isIstInvestition(): Boolean = istInvestition

    open fun isAktiv(): Boolean = aktiv

}
