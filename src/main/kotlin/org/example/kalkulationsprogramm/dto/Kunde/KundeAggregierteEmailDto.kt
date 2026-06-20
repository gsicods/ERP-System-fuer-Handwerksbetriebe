package org.example.kalkulationsprogramm.dto.Kunde

data class KundeAggregierteEmailDto(
    var email: String? = null,
    var isAusStammdaten: Boolean = false,
    var quellen: MutableList<KundeEmailQuelleDto> = ArrayList(),
)
