package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.math.BigDecimal
import java.time.LocalDate

@Entity
open class ProjektGeschaeftsdokument : ProjektDokument() {
    @Column(nullable = false)
    open var dokumentid: String? = null

    @Column(nullable = false)
    open var geschaeftsdokumentart: String? = null

    open var rechnungsdatum: LocalDate? = null

    open var faelligkeitsdatum: LocalDate? = null

    open var bruttoBetrag: BigDecimal? = null

    @Column(nullable = false)
    open var bezahlt: Boolean = false

    @Enumerated(EnumType.STRING)
    open var mahnstufe: Mahnstufe? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referenz_dokument_id")
    open var referenzDokument: ProjektGeschaeftsdokument? = null

    @OneToMany(mappedBy = "referenzDokument", fetch = FetchType.LAZY)
    open var mahnungen: MutableList<ProjektGeschaeftsdokument> = mutableListOf()

    @Column(name = "system_generiert", nullable = false)
    open var systemGeneriert: Boolean = false

    open fun isBezahlt(): Boolean = bezahlt

    open fun isSystemGeneriert(): Boolean = systemGeneriert

}
