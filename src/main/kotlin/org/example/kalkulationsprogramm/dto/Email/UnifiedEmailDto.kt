package org.example.kalkulationsprogramm.dto.Email

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class UnifiedEmailDto(
    var id: Long? = null,
    var messageId: String? = null,
    var fromAddress: String? = null,
    var senderDomain: String? = null,
    var recipient: String? = null,
    var cc: String? = null,
    var subject: String? = null,
    var body: String? = null,
    var htmlBody: String? = null,
    var sentAt: LocalDateTime? = null,
    var firstViewedAt: LocalDateTime? = null,
    @field:JsonProperty("isRead")
    var isRead: Boolean = false,
    @field:JsonProperty("isStarred")
    var isStarred: Boolean = false,
    var direction: String? = null,
    var zuordnungTyp: String? = null,
    var projektId: Long? = null,
    var projektName: String? = null,
    var anfrageId: Long? = null,
    var anfrageName: String? = null,
    var lieferantId: Long? = null,
    var lieferantName: String? = null,
    var kundeId: Long? = null,
    var kundeName: String? = null,
    var folder: String? = null,
    var spamScore: Int? = null,
    var parentEmailId: Long? = null,
    var replyCount: Int = 0,
    var threadLastActivityAt: LocalDateTime? = null,
    var attachments: List<AttachmentDto>? = null,
    var isHasAttachments: Boolean = false,
) {
    data class AttachmentDto(
        var id: Long? = null,
        var originalFilename: String? = null,
        var mimeType: String? = null,
        var fileSize: Long? = null,
        var contentId: String? = null,
        var isInline: Boolean = false,
    )
}
