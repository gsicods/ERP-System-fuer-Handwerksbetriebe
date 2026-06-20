package org.example.kalkulationsprogramm.dto

import org.example.kalkulationsprogramm.domain.KostenstellenTyp

data class KostenstelleDto(
    var id: Long? = null,
    var bezeichnung: String? = null,
    var typ: KostenstellenTyp? = null,
    var beschreibung: String? = null,
    var isIstFixkosten: Boolean = false,
    var isIstInvestition: Boolean = false,
    var isAktiv: Boolean = false,
    var sortierung: Int? = null,
)
