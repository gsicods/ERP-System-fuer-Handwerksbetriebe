package org.example.kalkulationsprogramm.dto.Leistung

import org.example.kalkulationsprogramm.domain.Verrechnungseinheit
import java.math.BigDecimal

data class LeistungCreateDto(
    var name: String? = null,
    var description: String? = null,
    var price: BigDecimal? = null,
    var unit: Verrechnungseinheit? = null,
    var folderId: Long? = null,
)
