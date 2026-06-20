package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "bwa_position")
open class BwaPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bwa_upload_id", nullable = false)
    open var bwaUpload: BwaUpload? = null

    @Column(length = 20)
    open var kontonummer: String? = null

    @Column(nullable = false)
    open var bezeichnung: String? = null

    @Column(precision = 14, scale = 2, nullable = false)
    open var betragMonat: BigDecimal? = null

    @Column(precision = 14, scale = 2)
    open var betragKumuliert: BigDecimal? = null

    @Column(length = 50)
    open var kategorie: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kostenstelle_id")
    open var kostenstelle: Kostenstelle? = null

    @Column(nullable = false)
    open var inRechnungenGefunden: Boolean? = false

    @Column(precision = 14, scale = 2)
    open var rechnungssumme: BigDecimal? = null

    @Column(precision = 14, scale = 2)
    open var differenz: BigDecimal? = null

    @Column(nullable = false)
    open var manuellKorrigiert: Boolean? = false

    @Column(length = 500)
    open var notiz: String? = null

}
