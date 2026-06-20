package org.example.kalkulationsprogramm.dto

data class ImportAnalysisResult(
    var existingCount: Int = 0,
    var newCount: Int = 0,
    var newArticleExamples: List<String>? = null,
)
