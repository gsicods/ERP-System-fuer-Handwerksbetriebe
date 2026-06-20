package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
open class Urlaubsantrag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mitarbeiter_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("abteilungen", "dokumente", "loginToken", "hibernateLazyInitializer", "handler")
    open var mitarbeiter: Mitarbeiter? = null

    @Column(nullable = false)
    open var vonDatum: LocalDate? = null

    @Column(nullable = false)
    open var bisDatum: LocalDate? = null

    @Column(length = 2000)
    open var bemerkung: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: Status? = Status.OFFEN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var typ: Typ? = Typ.URLAUB

    enum class Status {
        OFFEN, GENEHMIGT, ABGELEHNT, STORNIERT
    }

    enum class Typ {
        URLAUB, KRANKHEIT, FORTBILDUNG, ZEITAUSGLEICH, ARBEIT, PAUSE
    }

}
