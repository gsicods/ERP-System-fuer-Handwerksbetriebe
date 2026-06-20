package org.example.kalkulationsprogramm.dto.miete

data class MietobjektDto(
    var id: Long? = null,
    var name: String? = null,
    var strasse: String? = null,
    var plz: String? = null,
    var ort: String? = null,
)
