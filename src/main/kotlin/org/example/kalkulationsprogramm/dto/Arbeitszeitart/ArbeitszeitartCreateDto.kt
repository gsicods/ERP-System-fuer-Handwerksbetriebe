package org.example.kalkulationsprogramm.dto.Arbeitszeitart

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class ArbeitszeitartCreateDto(
    @field:NotBlank(message = "Bezeichnung ist erforderlich")
    var bezeichnung: String? = null,
    var beschreibung: String? = null,
    @field:NotNull(message = "Stundensatz ist erforderlich")
    @field:Positive(message = "Stundensatz muss positiv sein")
    var stundensatz: BigDecimal? = null,
    var isAktiv: Boolean = true,
    var sortierung: Int = 0,
)
