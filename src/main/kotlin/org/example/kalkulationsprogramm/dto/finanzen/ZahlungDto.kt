package org.example.kalkulationsprogramm.dto.finanzen

import org.example.kalkulationsprogramm.domain.ZahlungRichtung
import org.example.kalkulationsprogramm.domain.ZahlungStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ZahlungDto(
    val id: Long?,
    val richtung: ZahlungRichtung,
    val status: ZahlungStatus,
    val zahlungsdatum: LocalDate?,
    val betrag: BigDecimal,
    val zahlungsart: String?,
    val verwendungszweck: String?,
    val ausgangsDokumentId: Long?,
    val belegId: Long?,
    val erfasstAm: LocalDateTime?
)
