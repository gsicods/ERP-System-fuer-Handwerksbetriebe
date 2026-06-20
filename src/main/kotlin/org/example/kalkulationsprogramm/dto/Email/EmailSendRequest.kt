package org.example.kalkulationsprogramm.dto.Email

data class EmailSendRequest(
    var dokumentId: Long? = null,
    var recipient: String? = null,
    var cc: String? = null,
    var bauvorhaben: String? = null,
    var fromAddress: String? = null,
    var subject: String? = null,
    var htmlBody: String? = null,
    var benutzer: String? = null,
    var frontendUserId: Long? = null,
)
