package org.example.kalkulationsprogramm.dto.Email

data class EmailPreviewRequest(
    var dokumentId: Long? = null,
    var anrede: String? = null,
    var benutzer: String? = null,
    var position: String? = null,
    var bauvorhaben: String? = null,
    var frontendUserId: Long? = null,
)
