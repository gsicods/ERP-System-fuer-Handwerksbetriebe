package org.example.kalkulationsprogramm.dto.ProjektZeit

import java.math.BigDecimal

data class ZeitErfassenDto(
    var arbeitsgangID: Long? = null,
    var produktkategorieID: Long? = null,
    var anzahlInStunden: BigDecimal? = null,
)
