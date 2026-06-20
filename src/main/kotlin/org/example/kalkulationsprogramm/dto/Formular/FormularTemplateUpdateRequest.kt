package org.example.kalkulationsprogramm.dto.Formular

import jakarta.validation.constraints.NotBlank

data class FormularTemplateUpdateRequest(
    @field:NotBlank(message = "Der Vorlageninhalt darf nicht leer sein.")
    var html: String? = null,
)
