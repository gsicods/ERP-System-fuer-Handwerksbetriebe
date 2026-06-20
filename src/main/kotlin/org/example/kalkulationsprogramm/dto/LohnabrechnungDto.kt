package org.example.kalkulationsprogramm.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class LohnabrechnungDto(
    var id: Long? = null,
    var mitarbeiterId: Long? = null,
    var mitarbeiterName: String? = null,
    var steuerberaterId: Long? = null,
    var steuerberaterName: String? = null,
    var jahr: Int? = null,
    var monat: Int? = null,
    var originalDateiname: String? = null,
    var downloadUrl: String? = null,
    var bruttolohn: BigDecimal? = null,
    var nettolohn: BigDecimal? = null,
    var importDatum: LocalDateTime? = null,
    var status: String? = null,
)
