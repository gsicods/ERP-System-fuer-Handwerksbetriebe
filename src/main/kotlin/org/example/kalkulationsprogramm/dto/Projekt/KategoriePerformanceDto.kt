package org.example.kalkulationsprogramm.dto.Projekt

data class KategoriePerformanceDto(
    var kategorieName: String? = null,
    var umsatz: Double = 0.0,
    var gewinn: Double = 0.0,
    var stueckzahl: Long = 0,
    var umsatzVorjahr: Double = 0.0,
    var gewinnVorjahr: Double = 0.0,
    var stueckzahlVorjahr: Long = 0,
)
