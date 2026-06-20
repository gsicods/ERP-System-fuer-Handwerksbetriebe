package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "beleg_position")
open class BelegPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beleg_id", nullable = false)
    open var beleg: Beleg? = null

    @Column(nullable = false)
    open var sortierung: Int = 0

    @Column(nullable = false, length = 500)
    open var beschreibung: String? = null

    @Column(precision = 15, scale = 3)
    open var menge: BigDecimal? = null

    @Column(length = 20)
    open var einheit: String? = null

    @Column(precision = 15, scale = 4)
    open var einzelpreis: BigDecimal? = null

    @Column(name = "betrag_netto", precision = 15, scale = 2)
    open var betragNetto: BigDecimal? = null

    @Column(name = "betrag_brutto", precision = 15, scale = 2)
    open var betragBrutto: BigDecimal? = null

    @Column(name = "mwst_satz", precision = 5, scale = 2)
    open var mwstSatz: BigDecimal? = null

    @Column(name = "ist_fuer_firma", nullable = false)
    open var istFuerFirma: Boolean = false

    open fun isIstFuerFirma(): Boolean = istFuerFirma

    @Column(name = "erstellt_am", nullable = false)
    open var erstelltAm: LocalDateTime? = null

}
