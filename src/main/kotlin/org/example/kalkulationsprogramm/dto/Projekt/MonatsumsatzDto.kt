package org.example.kalkulationsprogramm.dto.Projekt

data class MonatsumsatzDto(
    var monat: Int = 0,
    var letztesJahr: Double = 0.0,
    var diesesJahr: Double = 0.0,
    var arbeitskosten: Double = 0.0,
    var materialkosten: Double = 0.0,
    var kosten: Double = 0.0,
    var arbeitskostenVorjahr: Double = 0.0,
    var materialkostenVorjahr: Double = 0.0,
    var kostenVorjahr: Double = 0.0,
    var lieferantenkosten: Double = 0.0,
    var lieferantenkostenVorjahr: Double = 0.0,
)
