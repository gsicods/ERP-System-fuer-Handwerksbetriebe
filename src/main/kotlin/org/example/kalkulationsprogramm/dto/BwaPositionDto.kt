package org.example.kalkulationsprogramm.dto

import java.math.BigDecimal

data class BwaPositionDto(
    var id: Long? = null,
    var kontonummer: String? = null,
    var bezeichnung: String? = null,
    var betragMonat: BigDecimal? = null,
    var betragKumuliert: BigDecimal? = null,
    var kategorie: String? = null,
    var kostenstelleId: Long? = null,
    var kostenstelleBezeichnung: String? = null,
    var inRechnungenGefunden: Boolean? = null,
    var rechnungssumme: BigDecimal? = null,
    var differenz: BigDecimal? = null,
    var manuellKorrigiert: Boolean? = null,
    var notiz: String? = null,
)
