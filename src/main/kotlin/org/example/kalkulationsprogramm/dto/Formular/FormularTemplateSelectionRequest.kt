package org.example.kalkulationsprogramm.dto.Formular

import jakarta.validation.constraints.NotBlank

data class FormularTemplateSelectionRequest(
    @field:NotBlank
    var dokumenttyp: String? = null,
    @field:NotBlank
    var templateName: String? = null,
    var userId: Long? = null,
    var userIds: List<Long>? = null,
)
