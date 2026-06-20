package org.example.kalkulationsprogramm.dto.Artikel

import org.example.kalkulationsprogramm.domain.Verrechnungseinheit
import java.math.BigDecimal

data class ArtikelCreateDto(
    var produktname: String? = null,
    var produktlinie: String? = null,
    var produkttext: String? = null,
    var externeArtikelnummer: String? = null,
    var verpackungseinheit: Long? = null,
    var preiseinheit: String? = null,
    var verrechnungseinheit: Verrechnungseinheit? = null,
    var kategorieId: Long? = null,
    var werkstoffId: Long? = null,
    var lieferantId: Long? = null,
    var preis: BigDecimal? = null,
)
