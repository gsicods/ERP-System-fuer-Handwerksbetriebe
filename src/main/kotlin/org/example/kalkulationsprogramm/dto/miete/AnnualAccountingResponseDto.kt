package org.example.kalkulationsprogramm.dto.miete

import java.math.BigDecimal

data class AnnualAccountingResponseDto(
    var mietobjektId: Long? = null,
    var mietobjektName: String? = null,
    var mietobjektStrasse: String? = null,
    var mietobjektPlz: String? = null,
    var mietobjektOrt: String? = null,
    var jahr: Int? = null,
    var gesamtkosten: BigDecimal? = null,
    var gesamtkostenVorjahr: BigDecimal? = null,
    var gesamtkostenDifferenz: BigDecimal? = null,
    var kostenstellen: MutableList<AnnualAccountingCostCenterDto> = ArrayList(),
    var parteien: MutableList<AnnualAccountingPartyDto> = ArrayList(),
    var verbrauchsvergleiche: MutableList<AnnualAccountingConsumptionDto> = ArrayList(),
)
