package org.example.kalkulationsprogramm.dto.ProjektEmail

data class ProjektEmailFileDto(
    var id: Long? = null,
    var originalFilename: String? = null,
    var storedFilename: String? = null,
    var contentId: String? = null,
    var isInline: Boolean = false,
    var url: String? = null,
)
