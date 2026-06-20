package org.example.kalkulationsprogramm.dto.Lieferant

data class LieferantResponseDto(
    var id: Long? = null,
    var lieferantenname: String? = null,
    var kundenEmails: List<String>? = null,
)
