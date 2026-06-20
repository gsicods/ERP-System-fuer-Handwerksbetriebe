package org.example.kalkulationsprogramm.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class GewerkDto(
    var id: Long? = null,
    @field:NotBlank(message = "Name darf nicht leer sein.")
    @field:Size(max = 255, message = "Name ist zu lang (max. 255 Zeichen).")
    var name: String? = null,
    @field:NotBlank(message = "BG-Name darf nicht leer sein.")
    @field:Size(max = 255, message = "BG-Name ist zu lang (max. 255 Zeichen).")
    var bgName: String? = null,
    @field:NotNull(message = "BG-Satz (Prozent) ist Pflicht.")
    @field:DecimalMin(value = "0.00", message = "BG-Satz darf nicht negativ sein.")
    @field:DecimalMax(value = "100.00", message = "BG-Satz darf nicht ueber 100 % liegen.")
    var bgSatzProzent: BigDecimal? = null,
    var aktiv: Boolean? = null,
    @field:Size(max = 500, message = "Bemerkung ist zu lang (max. 500 Zeichen).")
    var bemerkung: String? = null,
)
