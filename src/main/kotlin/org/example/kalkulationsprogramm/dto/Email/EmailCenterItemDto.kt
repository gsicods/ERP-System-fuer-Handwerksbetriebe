package org.example.kalkulationsprogramm.dto.Email

import org.example.kalkulationsprogramm.domain.EmailDirection
import java.time.LocalDateTime

data class EmailCenterItemDto(
    var type: String? = null,
    var id: Long? = null,
    var containerId: Long? = null,
    var sender: String? = null,
    var recipients: List<String>? = null,
    var subject: String? = null,
    var body: String? = null,
    var sentAt: LocalDateTime? = null,
    var attachments: List<EmailCenterAttachmentDto>? = null,
    var direction: EmailDirection? = null,
    var parentId: Long? = null,
    var replies: List<EmailCenterItemDto>? = null,
) {
    constructor(
        type: String?,
        id: Long?,
        sender: List<String>?,
        subject: String?,
        body: String?,
    ) : this(type = type, id = id, recipients = sender, subject = subject, body = body)

    constructor(
        type: String?,
        id: Long?,
        sender: String?,
        recipients: List<String>?,
        subject: String?,
        body: String?,
        sentAt: LocalDateTime?,
        attachments: List<EmailCenterAttachmentDto>?,
        direction: EmailDirection?,
        parentId: Long?,
        replies: List<EmailCenterItemDto>?,
    ) : this(type, id, null, sender, recipients, subject, body, sentAt, attachments, direction, parentId, replies)
}
