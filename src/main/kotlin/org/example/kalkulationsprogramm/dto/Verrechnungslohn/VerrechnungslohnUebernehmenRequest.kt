package org.example.kalkulationsprogramm.dto.Verrechnungslohn

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class VerrechnungslohnUebernehmenRequest(
    @field:Min(2000)
    @field:Max(2100)
    var jahr: Int = 0,
    @field:NotNull
    @field:DecimalMin(value = "0.01", message = "basisSatz muss positiv sein")
    var basisSatz: BigDecimal? = null,
    var abteilungAufschlaege: MutableList<AbteilungAufschlag> = ArrayList(),
) {
    data class AbteilungAufschlag(
        var abteilungId: Long? = null,
        var aufschlagEuro: BigDecimal = BigDecimal.ZERO,
    )
}
