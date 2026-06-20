package org.example.kalkulationsprogramm.dto.finanzen

import java.math.BigDecimal
import java.time.LocalDate

data class FinanzenDashboardDto(
    val von: LocalDate,
    val bis: LocalDate,
    val umsatzBrutto: BigDecimal,
    val eingangsKostenBrutto: BigDecimal,
    val zahlungseingaenge: BigDecimal,
    val zahlungsausgaenge: BigDecimal,
    val offenerAusgangBrutto: BigDecimal,
    val offeneEingangsBelegeBrutto: BigDecimal,
    val liquiditaet: BigDecimal,
    val ergebnisBrutto: BigDecimal,
    val offeneAusgangsrechnungen: Long,
    val offeneEingangsbelege: Long,
    val offenePosten: List<OffenerPostenDto>
)
