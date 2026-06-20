package org.example.kalkulationsprogramm.dto.Kunde

import org.example.kalkulationsprogramm.domain.EmailDirection
import java.time.LocalDateTime

data class KundeKommunikationDto(
    var id: Long? = null,
    var referenzId: Long? = null,
    var referenzTyp: String? = null,
    var referenzName: String? = null,
    var subject: String? = null,
    var absender: String? = null,
    var empfaenger: String? = null,
    var zeitpunkt: LocalDateTime? = null,
    var direction: EmailDirection? = null,
    var snippet: String? = null,
    var body: String? = null,
    var attachments: List<KundeEmailAttachmentDto>? = null,
    var parentEmailId: Long? = null,
    var replyCount: Int = 0,
)
