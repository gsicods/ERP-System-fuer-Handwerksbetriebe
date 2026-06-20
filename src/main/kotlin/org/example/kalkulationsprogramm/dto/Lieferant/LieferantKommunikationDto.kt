package org.example.kalkulationsprogramm.dto.Lieferant

import org.example.kalkulationsprogramm.domain.EmailDirection
import java.time.LocalDateTime

data class LieferantKommunikationDto(
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
    var attachments: List<LieferantAttachmentViewDto>? = null,
)
