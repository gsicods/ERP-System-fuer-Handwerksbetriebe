package org.example.kalkulationsprogramm.dto.Lieferant

data class LieferantSearchResponseDto(
    var lieferanten: List<LieferantListItemDto>? = null,
    var gesamt: Long = 0,
    var seite: Int = 0,
    var seitenGroesse: Int = 0,
)
