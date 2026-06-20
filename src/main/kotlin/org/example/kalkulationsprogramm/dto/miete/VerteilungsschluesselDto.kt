package org.example.kalkulationsprogramm.dto.miete

import org.example.kalkulationsprogramm.domain.miete.VerteilungsschluesselTyp

data class VerteilungsschluesselDto(
    var id: Long? = null,
    var mietobjektId: Long? = null,
    var name: String? = null,
    var beschreibung: String? = null,
    var typ: VerteilungsschluesselTyp? = null,
    var eintraege: MutableList<VerteilungsschluesselEintragDto> = ArrayList(),
)
