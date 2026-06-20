package org.example.kalkulationsprogramm.domain.miete

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "zaehlerstand")
open class Zaehlerstand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verbrauchsgegenstand_id", nullable = false)
    open var verbrauchsgegenstand: Verbrauchsgegenstand? = null

    @Column(name = "abrechnungs_jahr", nullable = false)
    open var abrechnungsJahr: Int? = null

    @Column(nullable = false)
    open var stichtag: LocalDate? = null

    @Column(nullable = false, precision = 19, scale = 4)
    open var stand: BigDecimal? = null

    @Column(precision = 19, scale = 4)
    open var verbrauch: BigDecimal? = null

    open var erfasstAm: OffsetDateTime? = OffsetDateTime.now()

    open var kommentar: String? = null

}
