package org.example.kalkulationsprogramm.dto.Kunde

data class KundeSearchResponseDto(
    var kunden: List<KundeListItemDto>? = null,
    var gesamt: Long = 0,
    var seite: Int = 0,
    var seitenGroesse: Int = 0,
)
