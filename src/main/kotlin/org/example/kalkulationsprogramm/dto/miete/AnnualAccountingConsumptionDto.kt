package org.example.kalkulationsprogramm.dto.miete

import org.example.kalkulationsprogramm.domain.miete.Verbrauchsart
import java.math.BigDecimal

data class AnnualAccountingConsumptionDto(
    var verbrauchsgegenstandId: Long? = null,
    var name: String? = null,
    var raumName: String? = null,
    var verbrauchsart: Verbrauchsart? = null,
    var einheit: String? = null,
    var verbrauchJahr: BigDecimal? = null,
    var verbrauchVorjahr: BigDecimal? = null,
    var differenz: BigDecimal? = null,
)
