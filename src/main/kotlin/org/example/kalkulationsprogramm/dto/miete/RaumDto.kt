package org.example.kalkulationsprogramm.dto.miete

import java.math.BigDecimal

data class RaumDto(
    var id: Long? = null,
    var mietobjektId: Long? = null,
    var name: String? = null,
    var beschreibung: String? = null,
    var flaecheQuadratmeter: BigDecimal? = null,
)
