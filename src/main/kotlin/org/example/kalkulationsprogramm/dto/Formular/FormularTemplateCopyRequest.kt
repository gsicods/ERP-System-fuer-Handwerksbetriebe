package org.example.kalkulationsprogramm.dto.Formular

import jakarta.validation.constraints.NotBlank

data class FormularTemplateCopyRequest(
    @field:NotBlank(message = "Neuer Vorlagenname darf nicht leer sein.")
    var newName: String? = null,
)
