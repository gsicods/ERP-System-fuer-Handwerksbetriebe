package org.example.kalkulationsprogramm.dto.Artikel

import java.math.BigDecimal
import java.time.LocalDate

data class ArtikelInProjektResponseDto(
    var id: Long? = null,
    var artikelId: Long? = null,
    var externeArtikelnummer: String? = null,
    var produktname: String? = null,
    var produkttext: String? = null,
    var stueckzahl: Int? = null,
    var meter: BigDecimal? = null,
    var kilogramm: BigDecimal? = null,
    var preisProStueck: BigDecimal? = null,
    var hinzugefuegtAm: LocalDate? = null,
    var isBestellt: Boolean = false,
    var bestelltAm: LocalDate? = null,
    var kommentar: String? = null,
    var lieferantName: String? = null,
    var werkstoffName: String? = null,
    var schnittForm: String? = null,
    var anschnittWinkelLinks: String? = null,
    var anschnittWinkelRechts: String? = null,
)
