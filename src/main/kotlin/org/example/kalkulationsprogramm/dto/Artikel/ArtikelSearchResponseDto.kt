package org.example.kalkulationsprogramm.dto.Artikel

data class ArtikelSearchResponseDto(
    var artikel: List<ArtikelResponseDto>? = null,
    var gesamt: Long = 0,
    var seite: Int = 0,
    var seitenGroesse: Int = 0,
)
