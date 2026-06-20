package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "arbeitszeitart")
open class Arbeitszeitart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false, length = 100)
    open var bezeichnung: String? = null

    @Column(columnDefinition = "TEXT")
    open var beschreibung: String? = null

    @Column(nullable = false, precision = 10, scale = 2)
    open var stundensatz: BigDecimal? = null

    @Column(nullable = false)
    open var aktiv: Boolean = true

    open fun isAktiv(): Boolean = aktiv

    @Column(nullable = false)
    open var sortierung: Int = 0

}
