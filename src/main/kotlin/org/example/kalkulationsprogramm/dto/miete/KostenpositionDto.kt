package org.example.kalkulationsprogramm.dto.miete

import org.example.kalkulationsprogramm.domain.miete.KostenpositionBerechnung
import java.math.BigDecimal
import java.time.LocalDate

data class KostenpositionDto(
    var id: Long? = null,
    var kostenstelleId: Long? = null,
    var abrechnungsJahr: Int? = null,
    var betrag: BigDecimal? = null,
    var berechnung: KostenpositionBerechnung? = null,
    var verbrauchsfaktor: BigDecimal? = null,
    var berechneterBetrag: BigDecimal? = null,
    var verbrauchsmenge: BigDecimal? = null,
    var beschreibung: String? = null,
    var belegNummer: String? = null,
    var buchungsdatum: LocalDate? = null,
    var verteilungsschluesselId: Long? = null,
)
