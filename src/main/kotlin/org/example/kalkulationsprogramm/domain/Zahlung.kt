package org.example.kalkulationsprogramm.domain

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
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "zahlung")
class Zahlung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    lateinit var richtung: ZahlungRichtung

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ZahlungStatus = ZahlungStatus.ERFASST

    @Column(nullable = false)
    var zahlungsdatum: LocalDate? = null

    @Column(nullable = false, precision = 15, scale = 2)
    var betrag: BigDecimal = BigDecimal.ZERO

    @Column(length = 80)
    var zahlungsart: String? = null

    @Column(length = 500)
    var verwendungszweck: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ausgangs_dokument_id")
    var ausgangsDokument: AusgangsGeschaeftsDokument? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beleg_id")
    var beleg: Beleg? = null

    @Column(nullable = false, updatable = false)
    var erfasstAm: LocalDateTime? = null

    @PrePersist
    fun onCreate() {
        if (erfasstAm == null) {
            erfasstAm = LocalDateTime.now()
        }
        if (zahlungsdatum == null) {
            zahlungsdatum = LocalDate.now()
        }
    }
}
