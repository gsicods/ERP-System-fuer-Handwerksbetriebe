package org.example.kalkulationsprogramm.dto.Lieferant

import java.math.BigDecimal
import java.time.LocalDate

data class LieferantArtikelpreisDto(
    var artikelId: Long? = null,
    var produktname: String? = null,
    var produkttext: String? = null,
    var werkstoff: String? = null,
    var externeArtikelnummer: String? = null,
    var preisAenderungsdatum: LocalDate? = null,
    var preis: BigDecimal? = null,
)
