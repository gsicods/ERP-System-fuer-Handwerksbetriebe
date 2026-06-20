package org.example.kalkulationsprogramm.dto.Arbeitsgang

import java.math.BigDecimal

data class ArbeitsgangStundensatzDto(
    var arbeitsgangId: Long? = null,
    var stundensatz: BigDecimal? = null,
)
