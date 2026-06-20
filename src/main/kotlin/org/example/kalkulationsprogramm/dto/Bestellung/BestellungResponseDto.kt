package org.example.kalkulationsprogramm.dto.Bestellung

import java.math.BigDecimal
import java.time.LocalDate

data class BestellungResponseDto(
    var id: Long? = null,
    var artikelId: Long? = null,
    var externeArtikelnummer: String? = null,
    var produktname: String? = null,
    var produkttext: String? = null,
    var werkstoffName: String? = null,
    var kategorieName: String? = null,
    var rootKategorieId: Int? = null,
    var rootKategorieName: String? = null,
    var stueckzahl: Int = 0,
    var menge: BigDecimal? = null,
    var einheit: String? = null,
    var projektId: Long? = null,
    var projektName: String? = null,
    var projektNummer: String? = null,
    var kundenName: String? = null,
    var lieferantName: String? = null,
    var lieferantId: Long? = null,
    var isBestellt: Boolean = false,
    var bestelltAm: LocalDate? = null,
    var kommentar: String? = null,
    var kilogramm: BigDecimal? = null,
    var gesamtKilogramm: BigDecimal? = null,
    var schnittForm: String? = null,
    var anschnittWinkelLinks: String? = null,
    var anschnittWinkelRechts: String? = null,
)
