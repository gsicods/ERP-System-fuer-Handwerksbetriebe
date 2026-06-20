package org.example.kalkulationsprogramm.dto.Formular

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FormularTemplateSaveRequest(
    @field:NotBlank(message = "Vorlagenname darf nicht leer sein.")
    @field:Size(max = 100, message = "Vorlagenname darf maximal 100 Zeichen lang sein.")
    var name: String? = null,
    @field:NotBlank(message = "Vorlageninhalt darf nicht leer sein.")
    var html: String? = null,
    var assignedDokumenttypen: List<String>? = null,
)
