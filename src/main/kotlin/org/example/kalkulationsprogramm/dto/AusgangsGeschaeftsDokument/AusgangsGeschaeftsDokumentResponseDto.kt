package org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument

import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import java.math.BigDecimal
import java.time.LocalDate

data class AusgangsGeschaeftsDokumentResponseDto(
    var id: Long? = null,
    var dokumentNummer: String? = null,
    var typ: AusgangsGeschaeftsDokumentTyp? = null,
    var datum: LocalDate? = null,
    var betreff: String? = null,
    var htmlInhalt: String? = null,
    var positionenJson: String? = null,
    var betragNetto: BigDecimal? = null,
    var betragBrutto: BigDecimal? = null,
    var mwstSatz: BigDecimal? = null,
    var mwstBetrag: BigDecimal? = null,
    var abschlagsNummer: Int? = null,
    var zahlungszielTage: Int? = null,
    var versandDatum: LocalDate? = null,
    var isGebucht: Boolean = false,
    var gebuchtAm: LocalDate? = null,
    var isStorniert: Boolean = false,
    var storniertAm: LocalDate? = null,
    var isDigitalAngenommen: Boolean = false,
    var isBearbeitbar: Boolean = false,
    var projektId: Long? = null,
    var projektBauvorhaben: String? = null,
    var projektnummer: String? = null,
    var anfrageId: Long? = null,
    var kundeId: Long? = null,
    var kundennummer: String? = null,
    var kundenName: String? = null,
    var rechnungsadresse: String? = null,
    var rechnungsadresseOverride: String? = null,
    var vorgaengerId: Long? = null,
    var vorgaengerNummer: String? = null,
    var erstelltVonId: Long? = null,
    var erstelltVonName: String? = null,
    var pdfUrl: String? = null,
)
