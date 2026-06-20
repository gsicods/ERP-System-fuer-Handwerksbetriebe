package org.example.kalkulationsprogramm.dto.Anfrage

data class AnfrageEmailRequest(
    var recipient: String? = null,
    var cc: String? = null,
    var anrede: String? = null,
    var benutzer: String? = null,
    var position: String? = null,
    var bauvorhaben: String? = null,
)
