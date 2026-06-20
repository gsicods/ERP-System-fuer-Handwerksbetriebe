package org.example.kalkulationsprogramm.dto.Lieferant

import java.time.LocalDateTime

data class LieferantNotizDto(
    var id: Long? = null,
    var text: String? = null,
    var erstelltAm: LocalDateTime? = null,
)
