package org.example.kalkulationsprogramm.dto.Mitarbeiter

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class MitarbeiterStundenlohnDto(
    var id: Long? = null,
    var mitarbeiterId: Long? = null,
    @field:NotNull(message = "Stundenlohn ist Pflicht.")
    @field:DecimalMin(value = "0.00", message = "Stundenlohn darf nicht negativ sein.")
    @field:DecimalMax(value = "10000.00", message = "Stundenlohn ist unrealistisch hoch (max. 10.000 EUR).")
    var stundenlohn: BigDecimal? = null,
    @field:NotNull(message = "Gueltig-ab-Datum ist Pflicht.")
    var gueltigAb: LocalDate? = null,
    @field:Size(max = 500, message = "Bemerkung ist zu lang (max. 500 Zeichen).")
    var bemerkung: String? = null,
)
