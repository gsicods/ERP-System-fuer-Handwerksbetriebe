package org.example.kalkulationsprogramm.dto.Schnittbilder

data class SchnittbildResponseDto(
    var id: Long? = null,
    var bildUrlSchnittbild: String? = null,
    var form: String? = null,
    var kategorieId: Int? = null,
)
