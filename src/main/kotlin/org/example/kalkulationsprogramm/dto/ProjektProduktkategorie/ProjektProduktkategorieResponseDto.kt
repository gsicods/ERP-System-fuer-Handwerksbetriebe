package org.example.kalkulationsprogramm.dto.ProjektProduktkategorie

import org.example.kalkulationsprogramm.dto.Produktkategroie.ProduktkategorieResponseDto
import java.math.BigDecimal

data class ProjektProduktkategorieResponseDto(
    var id: Long? = null,
    var produktkategorie: ProduktkategorieResponseDto? = null,
    var menge: BigDecimal? = null,
)
