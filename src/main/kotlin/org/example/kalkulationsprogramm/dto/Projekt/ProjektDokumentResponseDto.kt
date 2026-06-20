package org.example.kalkulationsprogramm.dto.Projekt

import java.math.BigDecimal
import java.time.LocalDate

data class ProjektDokumentResponseDto(
    var id: Long? = null,
    var originalDateiname: String? = null,
    var dateityp: String? = null,
    var url: String? = null,
    var netzwerkPfad: String? = null,
    var dokumentGruppe: String? = null,
    var uploadDatum: LocalDate? = null,
    var emailVersandDatum: LocalDate? = null,
    var anrede: String? = null,
    var rechnungsnummer: String? = null,
    var rechnungsdatum: LocalDate? = null,
    var faelligkeitsdatum: LocalDate? = null,
    var geschaeftsdokumentart: String? = null,
    var mahnstufe: String? = null,
    var referenzDokumentId: Long? = null,
    var referenzDokumentNummer: String? = null,
    var isMahnung: Boolean = false,
    var rechnungsbetrag: BigDecimal? = null,
    var isBezahlt: Boolean = false,
    var projektId: Long? = null,
    var projektAuftragsnummer: String? = null,
    var projektKunde: String? = null,
    var projektKategorie: String? = null,
    var projektArbeitskosten: BigDecimal? = null,
    var projektMaterialkosten: BigDecimal? = null,
    var projektKosten: BigDecimal? = null,
    var lieferantId: Long? = null,
    var lieferantenname: String? = null,
    var uploadedByVorname: String? = null,
    var uploadedByNachname: String? = null,
)
