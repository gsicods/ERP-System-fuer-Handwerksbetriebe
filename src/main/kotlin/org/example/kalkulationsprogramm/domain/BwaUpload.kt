package org.example.kalkulationsprogramm.domain

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
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "bwa_upload")
class BwaUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var typ: BwaTyp? = null

    @Column(nullable = false)
    var jahr: Int? = null

    var monat: Int? = null
    var originalDateiname: String? = null
    var gespeicherterDateiname: String? = null

    @Column(nullable = false)
    var uploadDatum: LocalDateTime? = null

    var analyseDatum: LocalDateTime? = null

    @Column(columnDefinition = "TEXT")
    var aiRawJson: String? = null

    var aiConfidence: Double? = null

    @Column(nullable = false)
    var analysiert: Boolean? = false

    @Column(nullable = false)
    var freigegeben: Boolean? = false

    var freigegebenAm: LocalDateTime? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freigegeben_von_id")
    var freigegebenVon: Mitarbeiter? = null

    @Column(precision = 14, scale = 2)
    var gesamtGemeinkosten: BigDecimal? = null

    @Column(precision = 14, scale = 2)
    var kostenAusRechnungen: BigDecimal? = null

    @Column(precision = 14, scale = 2)
    var kostenAusBwa: BigDecimal? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steuerberater_id")
    var steuerberater: SteuerberaterKontakt? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id")
    var sourceEmail: Email? = null

    @OneToMany(mappedBy = "bwaUpload", cascade = [CascadeType.ALL], orphanRemoval = true)
    var positionen: MutableList<BwaPosition> = ArrayList()

    @PrePersist
    protected fun onCreate() {
        if (uploadDatum == null) {
            uploadDatum = LocalDateTime.now()
        }
    }
}
