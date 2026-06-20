package org.example.kalkulationsprogramm.dto.miete

import java.math.BigDecimal

data class AnnualAccountingCostCenterDto(
    var kostenstelleId: Long? = null,
    var kostenstelleName: String? = null,
    var summe: BigDecimal? = null,
    var vorjahr: BigDecimal? = null,
    var differenz: BigDecimal? = null,
    var parteianteile: MutableList<AnnualAccountingShareDto> = ArrayList(),
)
