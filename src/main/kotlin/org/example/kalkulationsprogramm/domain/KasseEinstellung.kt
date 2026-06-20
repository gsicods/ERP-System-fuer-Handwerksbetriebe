package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "kasse_einstellung")
open class KasseEinstellung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false, precision = 10, scale = 2)
    open var mindestbestand: BigDecimal? = BigDecimal.ZERO

    @Column(name = "ehegattengehalt_aktiv", nullable = false)
    open var ehegattengehaltAktiv: Boolean = false

    @Column(name = "ehegattengehalt_betrag", precision = 10, scale = 2)
    open var ehegattengehaltBetrag: BigDecimal? = null

    @Column(name = "ehegattengehalt_tag")
    open var ehegattengehaltTag: Int? = null

    @Column(name = "ehegattengehalt_empfaenger_name", length = 120)
    open var ehegattengehaltEmpfaengerName: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privateinlage_sachkonto_id")
    open var privateinlageSachkonto: Sachkonto? = null

    @Column(name = "letzte_buchung_jahrmonat", length = 7)
    open var letzteBuchungJahrmonat: String? = null

    @Column(name = "aktualisiert_am")
    open var aktualisiertAm: LocalDateTime? = null

    open fun isEhegattengehaltAktiv(): Boolean = ehegattengehaltAktiv

}
