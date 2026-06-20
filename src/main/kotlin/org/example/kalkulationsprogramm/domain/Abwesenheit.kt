package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "abwesenheit")
open class Abwesenheit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mitarbeiter_id", nullable = false)
    open var mitarbeiter: Mitarbeiter? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "urlaubsantrag_id")
    open var urlaubsantrag: Urlaubsantrag? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    open var typ: AbwesenheitsTyp? = null

    @Column(nullable = false)
    open var datum: LocalDate? = null

    @Column(nullable = false, precision = 10, scale = 2)
    open var stunden: BigDecimal? = null

    @Column(length = 500)
    open var notiz: String? = null

}
