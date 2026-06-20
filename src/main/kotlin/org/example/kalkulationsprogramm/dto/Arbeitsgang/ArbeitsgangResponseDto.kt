package org.example.kalkulationsprogramm.dto.Arbeitsgang

import java.math.BigDecimal

data class ArbeitsgangResponseDto(
    var id: Long? = null,
    var beschreibung: String? = null,
    var stundensatz: BigDecimal? = null,
    var abteilungId: Long? = null,
    var abteilungName: String? = null,
    var stundensatzJahr: Int? = null,
)
