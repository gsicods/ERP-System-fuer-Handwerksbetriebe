package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "monats_saldo",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_monats_saldo_mitarbeiter_jahr_monat",
            columnNames = ["mitarbeiter_id", "jahr", "monat"]
        )
    ]
)
open class MonatsSaldo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mitarbeiter_id", nullable = false)
    open var mitarbeiter: Mitarbeiter? = null

    @Column(nullable = false)
    open var jahr: Int? = null

    @Column(nullable = false)
    open var monat: Int? = null

    @Column(nullable = false, precision = 10, scale = 2)
    open var istStunden: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false, precision = 10, scale = 2)
    open var sollStunden: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false, precision = 10, scale = 2)
    open var abwesenheitsStunden: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false, precision = 10, scale = 2)
    open var feiertagsStunden: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false, precision = 10, scale = 2)
    open var korrekturStunden: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false)
    open var gueltig: Boolean? = true

    @Column(nullable = false)
    open var berechnetAm: LocalDateTime? = null

    @PrePersist
    @PreUpdate
    open fun onSave() {
        if (berechnetAm == null) {
            berechnetAm = LocalDateTime.now()
        }
    }

    fun getGesamtIst(): BigDecimal =
        istStunden.add(abwesenheitsStunden).add(feiertagsStunden).add(korrekturStunden)

    fun getDifferenz(): BigDecimal = getGesamtIst().subtract(sollStunden)
}
