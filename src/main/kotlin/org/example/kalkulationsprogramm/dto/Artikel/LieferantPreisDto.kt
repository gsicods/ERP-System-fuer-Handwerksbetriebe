package org.example.kalkulationsprogramm.dto.Artikel

import java.math.BigDecimal

data class LieferantPreisDto(
    var lieferantId: Long? = null,
    var lieferantName: String? = null,
    var preis: BigDecimal? = null,
)
