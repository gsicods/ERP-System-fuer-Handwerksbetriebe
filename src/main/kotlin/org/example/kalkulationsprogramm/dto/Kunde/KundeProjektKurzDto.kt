package org.example.kalkulationsprogramm.dto.Kunde

import java.math.BigDecimal
import java.time.LocalDate

data class KundeProjektKurzDto(
    var id: Long? = null,
    var bauvorhaben: String? = null,
    var auftragsnummer: String? = null,
    var anlegedatum: LocalDate? = null,
    var abschlussdatum: LocalDate? = null,
    var isBezahlt: Boolean = false,
    var bruttoPreis: BigDecimal? = null,
)
