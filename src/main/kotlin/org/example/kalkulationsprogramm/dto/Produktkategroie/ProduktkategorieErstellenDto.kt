package org.example.kalkulationsprogramm.dto.Produktkategroie

import org.example.kalkulationsprogramm.domain.Verrechnungseinheit

data class ProduktkategorieErstellenDto(
    var bezeichnung: String? = null,
    var verrechnungseinheit: Verrechnungseinheit? = null,
    var parentId: Long? = null,
    var beschreibung: String? = null,
)
