package org.example.kalkulationsprogramm.dto.finanzen

import java.math.BigDecimal
import java.time.LocalDate

data class OffenerPostenDto(
    val typ: String,
    val id: Long?,
    val nummer: String?,
    val name: String?,
    val datum: LocalDate?,
    val faelligAm: LocalDate?,
    val brutto: BigDecimal,
    val bezahlt: BigDecimal,
    val offen: BigDecimal,
    val ueberfaellig: Boolean
)
