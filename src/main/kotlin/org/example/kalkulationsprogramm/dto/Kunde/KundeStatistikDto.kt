package org.example.kalkulationsprogramm.dto.Kunde

import java.math.BigDecimal
import java.time.LocalDate

data class KundeStatistikDto(
    var projektAnzahl: Long = 0,
    var anfrageAnzahl: Long = 0,
    var emailAdresseAnzahl: Long = 0,
    var letzteAktivitaet: LocalDate? = null,
    var gesamtUmsatz: BigDecimal? = null,
    var gesamtGewinn: BigDecimal? = null,
)
