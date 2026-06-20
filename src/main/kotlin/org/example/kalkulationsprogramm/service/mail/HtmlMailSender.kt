package org.example.kalkulationsprogramm.service.mail

import jakarta.mail.MessagingException
import java.io.File

interface HtmlMailSender {
    @Throws(MessagingException::class)
    fun send(
        fromAddress: String,
        toAddress: String,
        subject: String,
        htmlBody: String,
        inlineAttachments: Map<String, @JvmSuppressWildcards File>
    )
}
