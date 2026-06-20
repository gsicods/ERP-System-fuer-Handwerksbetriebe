package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
open class Feiertag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false)
    open var datum: LocalDate? = null

    @Column(nullable = false)
    open var bezeichnung: String? = null

    @Column(nullable = false, length = 10)
    open var bundesland: String? = "BY"

    @Column(nullable = false)
    open var halbTag: Boolean = false

    constructor()

    constructor(datum: LocalDate?, bezeichnung: String?) {
        this.datum = datum
        this.bezeichnung = bezeichnung
    }

    constructor(datum: LocalDate?, bezeichnung: String?, bundesland: String?) {
        this.datum = datum
        this.bezeichnung = bezeichnung
        this.bundesland = bundesland
    }

    open fun isHalbTag(): Boolean = halbTag

}
