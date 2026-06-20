package org.example.kalkulationsprogramm.dto.Materialkosten

import java.math.BigDecimal

data class MaterialkostenResponseDto(
    var id: Long? = null,
    var beschreibung: String? = null,
    var externeArtikelnummer: String? = null,
    var monat: Int? = null,
    var betrag: BigDecimal? = null,
    var rechnungsnummer: String? = null,
)
