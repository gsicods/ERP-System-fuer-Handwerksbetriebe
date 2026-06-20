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

@Entity
@Table(name = "verteilungsschluessel_eintrag")
open class VerteilungsschluesselEintrag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verteilungsschluessel_id", nullable = false)
    open var verteilungsschluessel: Verteilungsschluessel? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mietpartei_id", nullable = false)
    open var mietpartei: Mietpartei? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verbrauchsgegenstand_id")
    open var verbrauchsgegenstand: Verbrauchsgegenstand? = null

    @Column(nullable = false, precision = 10, scale = 4)
    open var anteil: BigDecimal? = null

    open var kommentar: String? = null

}
