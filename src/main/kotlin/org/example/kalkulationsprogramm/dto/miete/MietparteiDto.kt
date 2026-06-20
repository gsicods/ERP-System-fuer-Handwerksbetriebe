package org.example.kalkulationsprogramm.dto.miete

import org.example.kalkulationsprogramm.domain.miete.MietparteiRolle
import java.math.BigDecimal

data class MietparteiDto(
    var id: Long? = null,
    var name: String? = null,
    var rolle: MietparteiRolle? = null,
    var email: String? = null,
    var telefon: String? = null,
    var monatlicherVorschuss: BigDecimal? = null,
)
