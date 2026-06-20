package org.example.kalkulationsprogramm.dto.Zugferd

import java.math.BigDecimal
import java.time.LocalDate

data class ZugferdDaten(
    var kundenName: String? = null,
    var email: String? = null,
    var rechnungsnummer: String? = null,
    var rechnungsdatum: LocalDate? = null,
    var faelligkeitsdatum: LocalDate? = null,
    var betrag: BigDecimal? = null,
    var anrede: String? = null,
    var kundennummer: String? = null,
    var geschaeftsdokumentart: String? = null,
    var referenzDokumentId: Long? = null,
    var mahnstufe: String? = null,
    var betragNetto: BigDecimal? = null,
    var mwstSatz: BigDecimal? = null,
    var bereitsGezahlt: Boolean? = false,
    var skontoTage: Int? = null,
    var skontoProzent: BigDecimal? = null,
    var nettoTage: Int? = null,
    var bestellnummer: String? = null,
    var referenzNummer: String? = null,
    var artikelPositionen: MutableList<ZugferdArtikelPosition> = ArrayList(),
)
