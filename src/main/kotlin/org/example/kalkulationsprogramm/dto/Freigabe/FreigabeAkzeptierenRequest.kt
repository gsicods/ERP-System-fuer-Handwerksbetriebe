package org.example.kalkulationsprogramm.dto.Freigabe

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FreigabeAkzeptierenRequest(
    var email: String? = null,
    @field:NotBlank(message = "Vorname ist erforderlich.")
    @field:Size(min = 2, max = 80, message = "Vorname muss zwischen 2 und 80 Zeichen lang sein.")
    var vorname: String? = null,
    @field:NotBlank(message = "Nachname ist erforderlich.")
    @field:Size(min = 2, max = 80, message = "Nachname muss zwischen 2 und 80 Zeichen lang sein.")
    var nachname: String? = null,
    @field:NotBlank(message = "Unterzeichnername ist erforderlich.")
    @field:Size(min = 2, max = 160, message = "Unterzeichnername muss zwischen 2 und 160 Zeichen lang sein.")
    var unterzeichnerName: String? = null,
    var isBestaetigung: Boolean = false,
    var clientIp: String? = null,
    var userAgent: String? = null,
    var ausgewaehlteAlternativen: List<String>? = null,
)
