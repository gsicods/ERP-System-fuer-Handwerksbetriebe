package org.example.kalkulationsprogramm.dto.AnfrageEmail

data class AnfrageEmailAttachmentDto(
    var id: Long? = null,
    var originalFilename: String? = null,
    var storedFilename: String? = null,
    var contentId: String? = null,
    var isInline: Boolean = false,
)
