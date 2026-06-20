package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.util.Date
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "lieferanten_artikel_preise")
open class LieferantenArtikelPreise {
    @EmbeddedId
    open var id: LieferantenArtikelPreiseId? = LieferantenArtikelPreiseId()

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("artikelId")
    @JoinColumn(name = "artikel_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    open var artikel: Artikel? = null
        set(value) {
            field = value
            if (id == null) {
                id = LieferantenArtikelPreiseId()
            }
            id?.artikelId = null
        }

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lieferantId")
    @JoinColumn(name = "lieferant_id")
    open var lieferant: Lieferanten? = null
        set(value) {
            field = value
            if (id == null) {
                id = LieferantenArtikelPreiseId()
            }
            id?.lieferantId = value?.id
        }

    @Column(name = "externe_artikelnummer")
    open var externeArtikelnummer: String? = null

    open var preisAenderungsdatum: Date? = null

    @Column(precision = 19, scale = 2)
    open var preis: BigDecimal? = null

}
