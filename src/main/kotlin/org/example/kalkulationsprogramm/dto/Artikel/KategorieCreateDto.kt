package org.example.kalkulationsprogramm.dto.Artikel

data class KategorieCreateDto(
    var bezeichnung: String? = null,
    var parentId: Int? = null,
)
