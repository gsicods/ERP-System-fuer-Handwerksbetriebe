package org.example.kalkulationsprogramm.dto.Kunde

import java.math.BigDecimal
import java.time.LocalDate

data class KundeAnfrageKurzDto(
    var id: Long? = null,
    var bauvorhaben: String? = null,
    var anfragesnummer: String? = null,
    var anlegedatum: LocalDate? = null,
    var betrag: BigDecimal? = null,
)
