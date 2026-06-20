package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
open class ArtikelInProjekt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projekt_id")
    open var projekt: Projekt? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artikel_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    open var artikel: Artikel? = null

    @Column
    open var stueckzahl: Int? = null

    @Column(precision = 19, scale = 2)
    open var meter: BigDecimal? = null

    @Column(precision = 19, scale = 2)
    open var kilogramm: BigDecimal? = null

    @Column(precision = 19, scale = 2)
    open var preisProStueck: BigDecimal? = null

    @Column(nullable = false)
    open var hinzugefuegtAm: LocalDate? = null

    @Column(nullable = false)
    open var bestellt: Boolean = false

    open fun isBestellt(): Boolean = bestellt

    open var anschnittWinkelLinks: String? = null

    open var anschnittWinkelRechts: String? = null

    open var schnittForm: String? = null

    open var kommentar: String? = null

    open var bestelltAm: LocalDate? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_id")
    open var lieferant: Lieferanten? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "artikel_id", referencedColumnName = "artikel_id", insertable = false, updatable = false),
        JoinColumn(name = "lieferant_id", referencedColumnName = "lieferant_id", insertable = false, updatable = false)
    )
    open var lieferantenArtikelPreis: LieferantenArtikelPreise? = null

}
