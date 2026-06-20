package org.example.kalkulationsprogramm.dto.Zugferd

import java.math.BigDecimal

data class ZugferdArtikelPosition(
    var externeArtikelnummer: String? = null,
    var bezeichnung: String? = null,
    var menge: BigDecimal? = null,
    var mengeneinheit: String? = null,
    var einzelpreis: BigDecimal? = null,
    var preiseinheit: String? = null,
)
