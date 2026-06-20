package org.example.kalkulationsprogramm.dto.Lieferant

data class LieferantArtikelpreisPageDto(
    var artikelpreise: List<LieferantArtikelpreisDto>? = null,
    var gesamt: Long = 0,
    var seite: Int = 0,
    var seitenGroesse: Int = 0,
)
