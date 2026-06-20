package org.example.kalkulationsprogramm.dto.ProjektEmail

import org.example.kalkulationsprogramm.domain.EmailDirection
import java.time.LocalDateTime

data class ProjektEmailDto(
    var id: Long? = null,
    var direction: EmailDirection? = null,
    var from: String? = null,
    var to: String? = null,
    var subject: String? = null,
    var sentAt: LocalDateTime? = null,
    var bodyHtml: String? = null,
    var attachments: List<ProjektEmailFileDto>? = null,
    var benutzer: String? = null,
    var frontendUserId: Long? = null,
    var sender: String? = null,
    var body: String? = null,
    var recipients: List<String>? = null,
    var cc: List<String>? = null,
    var projektId: Long? = null,
    var anfrageId: Long? = null,
    var lieferantId: Long? = null,
    var parentEmailId: Long? = null,
    var replyCount: Int = 0,
)
