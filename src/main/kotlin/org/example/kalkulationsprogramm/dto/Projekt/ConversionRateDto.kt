package org.example.kalkulationsprogramm.dto.Projekt

data class ConversionRateDto(
    var jahr: Int = 0,
    var anfragenGesamt: Long = 0,
    var anfragenZuProjekt: Long = 0,
    var conversionRate: Double = 0.0,
)
