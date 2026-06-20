package org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument

import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class AbrechnungsverlaufDto(
    var basisdokumentId: Long? = null,
    var basisdokumentNummer: String? = null,
    var basisdokumentTyp: AusgangsGeschaeftsDokumentTyp? = null,
    var basisdokumentDatum: LocalDate? = null,
    var basisdokumentBetragNetto: BigDecimal? = null,
    var positionen: List<AbrechnungspositionDto>? = null,
    var bereitsAbgerechnet: BigDecimal? = null,
    var restbetrag: BigDecimal? = null,
    var bereitsAbgerechneteBlockIds: Set<String>? = null,
) {
    data class AbrechnungspositionDto(
        var id: Long? = null,
        var dokumentNummer: String? = null,
        var typ: AusgangsGeschaeftsDokumentTyp? = null,
        var datum: LocalDate? = null,
        var erstelltAm: LocalDateTime? = null,
        var betragNetto: BigDecimal? = null,
        var abschlagsNummer: Int? = null,
        var isStorniert: Boolean = false,
    )
}
