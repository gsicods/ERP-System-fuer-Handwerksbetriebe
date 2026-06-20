package org.example.kalkulationsprogramm.dto.miete

import java.math.BigDecimal

data class VerteilungsschluesselEintragDto(
    var id: Long? = null,
    var mietparteiId: Long? = null,
    var verbrauchsgegenstandId: Long? = null,
    var anteil: BigDecimal? = null,
    var kommentar: String? = null,
)
