package org.example.kalkulationsprogramm.dto.ProjektZeit

import org.example.kalkulationsprogramm.dto.Produktkategroie.ProduktkategorieResponseDto
import java.math.BigDecimal

data class ZeitResponseDto(
    var id: Long? = null,
    var arbeitsgangBeschreibung: String? = null,
    var produktkategorie: ProduktkategorieResponseDto? = null,
    var anzahlInStunden: BigDecimal? = null,
    var stundensatz: BigDecimal? = null,
    var mitarbeiterVorname: String? = null,
    var mitarbeiterNachname: String? = null,
)
