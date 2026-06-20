package org.example.kalkulationsprogramm.dto.ProjektProduktkategorie

import java.math.BigDecimal

data class ProjektProduktkategorieErfassenDto(
    var id: Long? = null,
    var produktkategorieID: Long? = null,
    var menge: BigDecimal? = null,
)
