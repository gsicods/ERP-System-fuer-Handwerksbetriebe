package org.example.kalkulationsprogramm.dto.miete

import java.math.BigDecimal
import java.time.LocalDate

data class ZaehlerstandDto(
    var id: Long? = null,
    var verbrauchsgegenstandId: Long? = null,
    var abrechnungsJahr: Int? = null,
    var stichtag: LocalDate? = null,
    var stand: BigDecimal? = null,
    var verbrauch: BigDecimal? = null,
    var kommentar: String? = null,
)
