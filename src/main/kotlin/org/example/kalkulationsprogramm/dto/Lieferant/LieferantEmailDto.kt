package org.example.kalkulationsprogramm.dto.Lieferant

import org.example.kalkulationsprogramm.domain.EmailDirection
import java.time.LocalDateTime

data class LieferantEmailDto(
    var id: Long? = null,
    var direction: EmailDirection? = null,
    var from: String? = null,
    var to: String? = null,
    var subject: String? = null,
    var bodyHtml: String? = null,
    var sentAt: LocalDateTime? = null,
    var attachments: List<LieferantEmailAttachmentDto>? = null,
)
