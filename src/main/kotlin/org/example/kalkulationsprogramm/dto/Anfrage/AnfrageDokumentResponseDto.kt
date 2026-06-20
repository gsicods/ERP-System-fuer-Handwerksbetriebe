package org.example.kalkulationsprogramm.dto.Anfrage

import java.math.BigDecimal
import java.time.LocalDate

data class AnfrageDokumentResponseDto(
    var id: Long? = null,
    var originalDateiname: String? = null,
    var gespeicherterDateiname: String? = null,
    var dateityp: String? = null,
    var url: String? = null,
    var thumbnailUrl: String? = null,
    var dokumentGruppe: String? = null,
    var uploadDatum: LocalDate? = null,
    var emailVersandDatum: LocalDate? = null,
    var anrede: String? = null,
    var rechnungsnummer: String? = null,
    var rechnungsdatum: LocalDate? = null,
    var faelligkeitsdatum: LocalDate? = null,
    var geschaeftsdokumentart: String? = null,
    var rechnungsbetrag: BigDecimal? = null,
    var isBezahlt: Boolean = false,
    var netzwerkPfad: String? = null,
)
