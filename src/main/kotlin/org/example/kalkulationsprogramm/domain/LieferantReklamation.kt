package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
open class LieferantReklamation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_id", nullable = false)
    open var lieferant: Lieferanten? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferschein_id")
    open var lieferschein: LieferantDokument? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erstellt_von_id")
    open var erstelltVon: Mitarbeiter? = null

    @Column(nullable = false)
    open var erstelltAm: LocalDateTime? = null

    @Column(columnDefinition = "TEXT")
    open var beschreibung: String? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    open var status: ReklamationStatus? = ReklamationStatus.OFFEN

    @OneToMany(mappedBy = "reklamation", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
    open var bilder: MutableList<LieferantBild> = mutableListOf()

}
