package org.example.kalkulationsprogramm.dto

data class EmailThreadDto(
    var rootEmailId: Long? = null,
    var focusedEmailId: Long? = null,
    var emails: List<EmailThreadEntryDto>? = null,
)
