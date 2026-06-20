package org.example.kalkulationsprogramm.dto.miete

import org.example.kalkulationsprogramm.domain.miete.MietparteiRolle
import java.math.BigDecimal

data class AnnualAccountingShareDto(
    var mietparteiId: Long? = null,
    var mietparteiName: String? = null,
    var rolle: MietparteiRolle? = null,
    var betrag: BigDecimal? = null,
)
