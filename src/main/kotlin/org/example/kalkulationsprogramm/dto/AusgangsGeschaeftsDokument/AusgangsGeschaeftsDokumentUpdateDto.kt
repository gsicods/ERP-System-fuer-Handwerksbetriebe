package org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument

import java.math.BigDecimal
import java.time.LocalDate

data class AusgangsGeschaeftsDokumentUpdateDto(
    var datum: LocalDate? = null,
    var betreff: String? = null,
    var betragNetto: BigDecimal? = null,
    var mwstSatz: BigDecimal? = null,
    var zahlungszielTage: Int? = null,
    var htmlInhalt: String? = null,
    var positionenJson: String? = null,
    var rechnungsadresseOverride: String? = null,
)
