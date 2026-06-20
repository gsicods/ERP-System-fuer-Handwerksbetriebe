package org.example.kalkulationsprogramm.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class EmailThreadEntryDto(
    var id: Long? = null,
    var subject: String? = null,
    var fromAddress: String? = null,
    var recipient: String? = null,
    var sentAt: String? = null,
    var direction: String? = null,
    var snippet: String? = null,
    var htmlBody: String? = null,
    var isForwarded: Boolean = false,
    @field:JsonProperty("isDraft")
    var isDraft: Boolean = false,
    var draftId: Long? = null,
    var attachments: List<AttachmentDto>? = null,
) {
    data class AttachmentDto(
        var id: Long? = null,
        var originalFilename: String? = null,
        var mimeType: String? = null,
        var sizeBytes: Long? = null,
        var contentId: String? = null,
        var isInline: Boolean = false,
    )
}
