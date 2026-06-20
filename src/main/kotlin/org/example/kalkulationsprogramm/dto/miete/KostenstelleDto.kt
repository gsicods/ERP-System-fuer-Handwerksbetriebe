package org.example.kalkulationsprogramm.dto.miete

data class KostenstelleDto(
    var id: Long? = null,
    var mietobjektId: Long? = null,
    var name: String? = null,
    var beschreibung: String? = null,
    var isUmlagefaehig: Boolean = true,
    var standardSchluesselId: Long? = null,
)
