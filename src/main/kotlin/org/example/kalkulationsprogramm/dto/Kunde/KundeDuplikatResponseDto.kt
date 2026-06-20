package org.example.kalkulationsprogramm.dto.Kunde

data class KundeDuplikatResponseDto(
    var duplikate: List<KundeDuplikatTrefferDto>? = null,
    var isHarterTreffer: Boolean = false,
)
