package org.example.kalkulationsprogramm.dto.Bestellung

data class BestellungEmailSendRequest(
    var lieferantId: Long? = null,
    var projektId: Long? = null,
    var recipient: String? = null,
    var cc: String? = null,
    var fromAddress: String? = null,
    var subject: String? = null,
    var htmlBody: String? = null,
    var benutzer: String? = null,
    var frontendUserId: Long? = null,
)
