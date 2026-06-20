package org.example.kalkulationsprogramm.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class KrankenkasseDto(
    var id: Long? = null,
    @field:NotBlank(message = "Name darf nicht leer sein.")
    @field:Size(max = 255, message = "Name ist zu lang (max. 255 Zeichen).")
    var name: String? = null,
    @field:Size(max = 32, message = "Kuerzel ist zu lang (max. 32 Zeichen).")
    var kuerzel: String? = null,
    @field:NotNull(message = "Zusatzbeitrag (Prozent) ist Pflicht.")
    @field:DecimalMin(value = "0.00", message = "Zusatzbeitrag darf nicht negativ sein.")
    @field:DecimalMax(value = "100.00", message = "Zusatzbeitrag darf nicht ueber 100 % liegen.")
    var zusatzbeitragProzent: BigDecimal? = null,
    var aktiv: Boolean? = null,
    var gueltigAb: LocalDate? = null,
    @field:Size(max = 500, message = "Bemerkung ist zu lang (max. 500 Zeichen).")
    var bemerkung: String? = null,
)
