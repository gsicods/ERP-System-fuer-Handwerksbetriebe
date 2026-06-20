package org.example.kalkulationsprogramm.dto.AnfrageEmail

import org.example.kalkulationsprogramm.domain.EmailDirection
import java.time.LocalDateTime

data class AnfrageEmailDto(
    var id: Long? = null,
    var sender: String? = null,
    var recipients: List<String>? = null,
    var subject: String? = null,
    var body: String? = null,
    var sentAt: LocalDateTime? = null,
    var attachments: List<AnfrageEmailAttachmentDto>? = null,
    var direction: EmailDirection? = null,
    var parentId: Long? = null,
    var replies: List<AnfrageEmailDto>? = null,
    var benutzer: String? = null,
    var frontendUserId: Long? = null,
)
