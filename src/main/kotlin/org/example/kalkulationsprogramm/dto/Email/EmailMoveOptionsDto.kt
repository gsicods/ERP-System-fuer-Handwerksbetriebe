package org.example.kalkulationsprogramm.dto.Email

import org.example.kalkulationsprogramm.dto.Anfrage.AnfrageOptionDto
import org.example.kalkulationsprogramm.dto.Projekt.ProjektOptionDto

data class EmailMoveOptionsDto(
    var anfragen: List<AnfrageOptionDto>? = null,
    var projekte: List<ProjektOptionDto>? = null,
)
