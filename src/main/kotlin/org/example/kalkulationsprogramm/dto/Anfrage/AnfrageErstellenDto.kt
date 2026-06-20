package org.example.kalkulationsprogramm.dto.Anfrage

import java.math.BigDecimal
import java.time.LocalDate

data class AnfrageErstellenDto(
    var bauvorhaben: String? = null,
    var kunde: String? = null,
    var kundenId: Long? = null,
    var kundennummer: String? = null,
    var kundenEmails: List<String>? = null,
    var anlegedatum: LocalDate? = null,
    var projektStrasse: String? = null,
    var projektPlz: String? = null,
    var projektOrt: String? = null,
    var kurzbeschreibung: String? = null,
    var abgeschlossen: Boolean? = null,
    var betrag: BigDecimal? = null,
)
