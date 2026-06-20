package org.example.kalkulationsprogramm.dto.Formular

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FormularTemplateRenameRequest(
    @field:NotBlank(message = "Neuer Vorlagenname darf nicht leer sein.")
    @field:Size(max = 100, message = "Vorlagenname darf maximal 100 Zeichen lang sein.")
    var newName: String? = null,
)
