package org.example.kalkulationsprogramm.dto.Formular

data class FormularTemplateDto(
    var html: String? = null,
    var lastModified: String? = null,
    var placeholders: List<String>? = null,
    var assignedDokumenttypen: List<String>? = null,
    var assignedUserIds: List<Long>? = null,
    var name: String? = null,
    var created: String? = null,
    var modified: String? = null,
)
