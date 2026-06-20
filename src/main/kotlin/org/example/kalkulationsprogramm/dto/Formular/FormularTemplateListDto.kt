package org.example.kalkulationsprogramm.dto.Formular

data class FormularTemplateListDto(
    var name: String? = null,
    var created: String? = null,
    var modified: String? = null,
    var assignedDokumenttypen: List<String>? = null,
)
