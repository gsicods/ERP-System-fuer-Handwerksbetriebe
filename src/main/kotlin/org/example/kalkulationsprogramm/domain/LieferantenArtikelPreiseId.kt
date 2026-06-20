package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class LieferantenArtikelPreiseId(
    var artikelId: Long? = null,
    var lieferantId: Long? = null,
) : Serializable
