package org.example.kalkulationsprogramm.dto.Projekt

data class KategorieUmsatzVergleichDto(
    var kategorie: String? = null,
    var letztesJahr: Long = 0,
    var diesesJahr: Long = 0,
    var verrechnungseinheit: String? = null,
)
