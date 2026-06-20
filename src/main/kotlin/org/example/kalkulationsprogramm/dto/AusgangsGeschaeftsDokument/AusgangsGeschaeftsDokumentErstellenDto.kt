package org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument

import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import java.math.BigDecimal
import java.time.LocalDate

data class AusgangsGeschaeftsDokumentErstellenDto(
    var typ: AusgangsGeschaeftsDokumentTyp? = null,
    var datum: LocalDate? = null,
    var betreff: String? = null,
    var betragNetto: BigDecimal? = null,
    var mwstSatz: BigDecimal? = null,
    var zahlungszielTage: Int? = null,
    var htmlInhalt: String? = null,
    var positionenJson: String? = null,
    var projektId: Long? = null,
    var anfrageId: Long? = null,
    var kundeId: Long? = null,
    var vorgaengerId: Long? = null,
    var erstelltVonId: Long? = null,
    var rechnungsadresseOverride: String? = null,
)
