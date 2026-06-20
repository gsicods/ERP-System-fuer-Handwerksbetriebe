package org.example.kalkulationsprogramm.dto.Projekt

data class UmsatzStatistikDto(
    var kategorien: List<KategorieUmsatzVergleichDto>? = null,
    var monatsUmsaetze: List<MonatsumsatzDto>? = null,
    var konversion: ConversionRateDto? = null,
    var ortHeatmap: List<OrtHeatmapDto>? = null,
    var kategoriePerformance: List<KategoriePerformanceDto>? = null,
    var topKunden: List<TopKundeDto>? = null,
)
