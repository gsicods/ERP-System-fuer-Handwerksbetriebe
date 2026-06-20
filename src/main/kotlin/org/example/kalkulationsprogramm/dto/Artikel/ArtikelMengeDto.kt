package org.example.kalkulationsprogramm.dto.Artikel

import java.math.BigDecimal

data class ArtikelMengeDto(
    var artikelId: Long? = null,
    var menge: BigDecimal? = null,
    var einheit: String? = null,
    var ausLager: Boolean? = null,
    var kommentar: String? = null,
    var lieferantId: Long? = null,
    var preis: BigDecimal? = null,
    var laengeProStueck: BigDecimal? = null,
    var stueckzahl: Int? = null,
    var schnittForm: String? = null,
    var anschnittWinkelLinks: String? = null,
    var anschnittWinkelRechts: String? = null,
)
