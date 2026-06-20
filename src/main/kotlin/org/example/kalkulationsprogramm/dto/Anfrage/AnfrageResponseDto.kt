package org.example.kalkulationsprogramm.dto.Anfrage

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class AnfrageResponseDto(
    var id: Long? = null,
    var kundenId: Long? = null,
    var kundenName: String? = null,
    var bauvorhaben: String? = null,
    var kundennummer: String? = null,
    var anfragesnummer: String? = null,
    var betrag: BigDecimal? = null,
    var kundenEmails: List<String>? = null,
    var emailVersandDatum: LocalDate? = null,
    var projektId: Long? = null,
    var anlegedatum: LocalDate? = null,
    var bildUrl: String? = null,
    var projektStrasse: String? = null,
    var projektPlz: String? = null,
    var projektOrt: String? = null,
    var kurzbeschreibung: String? = null,
    var isAbgeschlossen: Boolean = false,
    var createdAt: LocalDateTime? = null,
    var kundenStrasse: String? = null,
    var kundenPlz: String? = null,
    var kundenOrt: String? = null,
    var kundenTelefon: String? = null,
    var kundenMobiltelefon: String? = null,
    var kundenAnsprechpartner: String? = null,
    var kundenAnrede: String? = null,
)
