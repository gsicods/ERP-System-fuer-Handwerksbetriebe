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
@Table(name = "verbrauchsgegenstand")
open class Verbrauchsgegenstand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raum_id", nullable = false)
    open var raum: Raum? = null

    @Column(nullable = false)
    open var name: String? = null

    open var seriennummer: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var verbrauchsart: Verbrauchsart? = null

    open var einheit: String? = null

    open var aktiv: Boolean = true

    open fun isAktiv(): Boolean = aktiv

    @OneToMany(mappedBy = "verbrauchsgegenstand", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var zaehlerstaende: MutableList<Zaehlerstand> = mutableListOf()

}
