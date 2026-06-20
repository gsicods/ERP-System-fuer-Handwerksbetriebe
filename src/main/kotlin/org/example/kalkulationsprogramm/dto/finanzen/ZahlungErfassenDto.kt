package org.example.kalkulationsprogramm.dto.finanzen

import java.math.BigDecimal
import java.time.LocalDate

data class ZahlungErfassenDto(
    val betrag: BigDecimal?,
    val zahlungsdatum: LocalDate?,
    val zahlungsart: String?,
    val verwendungszweck: String?
)
