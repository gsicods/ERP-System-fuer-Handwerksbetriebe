package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "projekt_notiz")
open class ProjektNotiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projekt_id", nullable = false)
    open var projekt: Projekt? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mitarbeiter_id", nullable = false)
    open var mitarbeiter: Mitarbeiter? = null

    @Column(length = 4000, nullable = false)
    open var notiz: String? = null

    @Column(nullable = false)
    open var nurFuerErsteller: Boolean = false

    open fun isNurFuerErsteller(): Boolean = nurFuerErsteller

    @Column(nullable = false)
    open var mobileSichtbar: Boolean = true

    open fun isMobileSichtbar(): Boolean = mobileSichtbar

    @Column(nullable = false)
    open var erstelltAm: LocalDateTime? = null

    @OneToMany(mappedBy = "notiz", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true, fetch = FetchType.LAZY)
    open var bilder: MutableList<ProjektNotizBild> = mutableListOf()

}
