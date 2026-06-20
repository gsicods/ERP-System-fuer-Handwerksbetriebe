package org.example.kalkulationsprogramm.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class SvSatzDto(
    var id: Long? = null,
    @field:NotBlank(message = "Satz-Typ ist Pflicht.")
    var satzTyp: String? = null,
    @field:NotNull(message = "Prozent ist Pflicht.")
    @field:DecimalMin(value = "0.00", message = "Prozent darf nicht negativ sein.")
    @field:DecimalMax(value = "100.00", message = "Prozent darf nicht ueber 100 % liegen.")
    var prozent: BigDecimal? = null,
    @field:NotNull(message = "Gueltig-ab-Datum ist Pflicht.")
    var gueltigAb: LocalDate? = null,
    @field:Size(max = 500, message = "Beschreibung ist zu lang (max. 500 Zeichen).")
    var beschreibung: String? = null,
)
