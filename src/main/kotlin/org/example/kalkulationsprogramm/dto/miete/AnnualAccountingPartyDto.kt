package org.example.kalkulationsprogramm.dto.miete

import org.example.kalkulationsprogramm.domain.miete.MietparteiRolle
import java.math.BigDecimal

data class AnnualAccountingPartyDto(
    var mietparteiId: Long? = null,
    var mietparteiName: String? = null,
    var rolle: MietparteiRolle? = null,
    var summe: BigDecimal? = null,
    var vorjahr: BigDecimal? = null,
    var differenz: BigDecimal? = null,
    var monatlicherVorschuss: BigDecimal? = null,
    var jahresVorauszahlung: BigDecimal? = null,
    var saldo: BigDecimal? = null,
)
