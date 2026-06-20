package org.example.kalkulationsprogramm.dto.Projekt

data class OrtHeatmapDto(
    var ort: String? = null,
    var plz: String? = null,
    var projekte: Long = 0,
    var umsatz: Double = 0.0,
    var anteil: Double = 0.0,
)
