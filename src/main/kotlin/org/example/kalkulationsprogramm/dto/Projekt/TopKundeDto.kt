package org.example.kalkulationsprogramm.dto.Projekt

data class TopKundeDto(
    var kundenName: String? = null,
    var kundenId: Long? = null,
    var umsatz: Double = 0.0,
    var projektAnzahl: Long = 0,
    var gewinn: Double = 0.0,
)
