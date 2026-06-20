package org.example.kalkulationsprogramm.dto.Produktkategroie

import com.fasterxml.jackson.annotation.JsonProperty

data class ProduktkategorieAnalyseDto(
    var projektAnzahl: Long = 0,
    var durchschnittlicheZeit: Double = 0.0,
    var fixzeit: Double = 0.0,
    var steigung: Double = 0.0,
    var verrechnungseinheit: String? = null,
    var projekte: List<ProjektAnalyseDto>? = null,
    var arbeitsgangAnalysen: List<ArbeitsgangAnalyseDto>? = null,
    var datenpunkte: Int = 0,
    @field:JsonProperty("rQuadrat")
    var rQuadrat: Double = 0.0,
    var residualStdAbweichung: Double = 0.0,
)
