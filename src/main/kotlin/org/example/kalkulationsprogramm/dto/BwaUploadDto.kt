package org.example.kalkulationsprogramm.dto

import org.example.kalkulationsprogramm.domain.BwaTyp
import java.math.BigDecimal
import java.time.LocalDateTime

data class BwaUploadDto(
    var id: Long? = null,
    var typ: BwaTyp? = null,
    var jahr: Int? = null,
    var monat: Int? = null,
    var originalDateiname: String? = null,
    var pdfUrl: String? = null,
    var uploadDatum: LocalDateTime? = null,
    var analyseDatum: LocalDateTime? = null,
    var aiConfidence: Double? = null,
    var analysiert: Boolean? = null,
    var freigegeben: Boolean? = null,
    var freigegebenAm: LocalDateTime? = null,
    var freigegebenVonName: String? = null,
    var gesamtGemeinkosten: BigDecimal? = null,
    var kostenAusRechnungen: BigDecimal? = null,
    var kostenAusBwa: BigDecimal? = null,
    var steuerberaterName: String? = null,
    var positionen: List<BwaPositionDto>? = null,
)
