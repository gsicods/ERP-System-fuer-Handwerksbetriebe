package org.example.kalkulationsprogramm.dto.miete

import org.example.kalkulationsprogramm.domain.miete.Verbrauchsart

data class VerbrauchsgegenstandDto(
    var id: Long? = null,
    var raumId: Long? = null,
    var name: String? = null,
    var seriennummer: String? = null,
    var verbrauchsart: Verbrauchsart? = null,
    var einheit: String? = null,
    var isAktiv: Boolean = false,
)
