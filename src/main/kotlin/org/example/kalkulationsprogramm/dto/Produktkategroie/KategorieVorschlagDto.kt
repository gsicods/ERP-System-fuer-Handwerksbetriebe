package org.example.kalkulationsprogramm.dto.Produktkategroie

import org.example.kalkulationsprogramm.domain.Verrechnungseinheit
import java.math.BigDecimal

data class KategorieVorschlagDto(
    var kategorieId: Long? = null,
    var bezeichnung: String? = null,
    var pfad: String? = null,
    var verrechnungseinheit: Verrechnungseinheit? = null,
    var menge: BigDecimal? = null,
    var quelle: String? = null,
)
