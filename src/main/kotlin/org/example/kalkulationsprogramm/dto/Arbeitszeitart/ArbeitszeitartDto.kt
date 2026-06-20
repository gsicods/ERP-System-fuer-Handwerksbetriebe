package org.example.kalkulationsprogramm.dto.Arbeitszeitart

import java.math.BigDecimal

data class ArbeitszeitartDto(
    var id: Long? = null,
    var bezeichnung: String? = null,
    var beschreibung: String? = null,
    var stundensatz: BigDecimal? = null,
    var isAktiv: Boolean = false,
    var sortierung: Int = 0,
)
