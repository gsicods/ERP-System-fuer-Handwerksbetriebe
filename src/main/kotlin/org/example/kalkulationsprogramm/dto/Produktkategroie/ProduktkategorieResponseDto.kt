package org.example.kalkulationsprogramm.dto.Produktkategroie

import org.example.kalkulationsprogramm.domain.Verrechnungseinheit

data class ProduktkategorieResponseDto(
    var id: Long? = null,
    var bezeichnung: String? = null,
    var bildUrl: String? = null,
    var beschreibung: String? = null,
    var verrechnungseinheit: Verrechnungseinheit? = null,
    var pfad: String? = null,
    var isLeaf: Boolean = false,
    var projektAnzahl: Long? = null,
    var parentId: Long? = null,
)
