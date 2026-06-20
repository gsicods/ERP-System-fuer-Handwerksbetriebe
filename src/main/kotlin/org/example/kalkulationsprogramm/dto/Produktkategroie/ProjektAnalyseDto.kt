package org.example.kalkulationsprogramm.dto.Produktkategroie

data class ProjektAnalyseDto(
    var id: Long? = null,
    var projektname: String? = null,
    var bildUrl: String? = null,
    var auftragsnummer: String? = null,
    var kunde: String? = null,
    var masseinheit: Double = 0.0,
    var zeitGesamt: Double = 0.0,
    var arbeitsgaenge: List<ProjektArbeitsgangAnalyseDto>? = null,
)
