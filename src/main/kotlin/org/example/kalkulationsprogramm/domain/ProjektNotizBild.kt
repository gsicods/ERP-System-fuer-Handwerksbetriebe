package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "projekt_notiz_bild")
open class ProjektNotizBild {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notiz_id", nullable = false)
    open var notiz: ProjektNotiz? = null

    @Column(nullable = false)
    open var gespeicherterDateiname: String? = null

    @Column
    open var originalDateiname: String? = null

    @Column
    open var dateityp: String? = null

    @Column(nullable = false)
    open var erstelltAm: LocalDateTime? = null

}
