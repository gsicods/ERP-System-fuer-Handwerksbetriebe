package org.example.kalkulationsprogramm.dto.Lieferant

import java.time.LocalDateTime

data class LieferantStatistikDto(
    var artikelAnzahl: Int = 0,
    var emailAnzahl: Long = 0,
    var letzteEmail: LocalDateTime? = null,
    var emailDomains: List<String>? = null,
    var bestellungAnzahl: Int = 0,
    var lieferzeit: Int? = null,
    var gesamtKosten: Double? = null,
)
