package org.example.kalkulationsprogramm.dto.Materialkosten

import java.math.BigDecimal

data class MaterialkostenErfassenDto(
    var beschreibung: String? = null,
    var externeArtikelnummer: String? = null,
    var monat: Int? = null,
    var betrag: BigDecimal? = null,
    var lieferantId: Long? = null,
    var rechnungsnummer: String? = null,
)
