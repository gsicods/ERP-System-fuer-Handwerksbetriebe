package org.example.kalkulationsprogramm.dto

data class SteuerberaterAnsprechpartnerDto(
    var id: Long? = null,
    var anrede: String? = null,
    var vorname: String? = null,
    var nachname: String? = null,
    var email: String? = null,
    var telefon: String? = null,
    var istLohnAnsprechpartner: Boolean? = null,
    var notizen: String? = null,
)
