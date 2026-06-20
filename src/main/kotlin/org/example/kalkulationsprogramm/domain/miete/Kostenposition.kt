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
import java.time.LocalDate
import org.example.kalkulationsprogramm.domain.LieferantDokument

@Entity
@Table(name = "kostenposition")
open class Kostenposition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kostenstelle_id", nullable = false)
    open var kostenstelle: Kostenstelle? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verteilungsschluessel_id")
    open var verteilungsschluesselOverride: Verteilungsschluessel? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "berechnung")
    open var berechnung: KostenpositionBerechnung? = KostenpositionBerechnung.BETRAG

    @Column(name = "verbrauchsfaktor", precision = 19, scale = 6)
    open var verbrauchsfaktor: BigDecimal? = null

    @Column(nullable = false)
    open var abrechnungsJahr: Int? = null

    @Column(precision = 19, scale = 2)
    open var betrag: BigDecimal? = null

    open var beschreibung: String? = null

    open var belegNummer: String? = null

    open var buchungsdatum: LocalDate? = null

}
