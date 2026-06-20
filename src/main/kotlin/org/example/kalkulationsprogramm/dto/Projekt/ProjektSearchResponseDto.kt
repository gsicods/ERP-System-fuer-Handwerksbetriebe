package org.example.kalkulationsprogramm.dto.Projekt

data class ProjektSearchResponseDto(
    var projekte: List<ProjektResponseDto>? = null,
    var gesamt: Long = 0,
    var seite: Int = 0,
    var seitenGroesse: Int = 0,
)
