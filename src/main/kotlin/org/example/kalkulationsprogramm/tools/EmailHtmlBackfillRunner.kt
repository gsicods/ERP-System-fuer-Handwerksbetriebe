package org.example.kalkulationsprogramm.tools

import org.example.kalkulationsprogramm.repository.EmailRepository
import org.example.kalkulationsprogramm.util.EmailHtmlSanitizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.function.Consumer

@Component
class EmailHtmlBackfillRunner(
    private val emailRepository: EmailRepository
) {
    @Transactional
    fun run() {
        val updated = backfillEmails()
        log.info("HTML-Backfill abgeschlossen: {} E-Mails aktualisiert.", updated)
    }

    private fun backfillEmails(): Int {
        val changed = mutableListOf<org.example.kalkulationsprogramm.domain.Email>()
        for (email in emailRepository.findAll()) {
            if (refreshBodies(
                    prop(email, "rawBody"),
                    prop(email, "htmlBody"),
                    prop(email, "body"),
                    Consumer { setProp(email, "htmlBody", it) },
                    Consumer { setProp(email, "body", it) }
                )
            ) {
                changed += email
            }
        }
        if (changed.isNotEmpty()) {
            emailRepository.saveAll(changed)
        }
        return changed.size
    }

    private fun refreshBodies(
        rawBody: String?,
        currentHtml: String?,
        currentPlain: String?,
        htmlSetter: Consumer<String>,
        plainSetter: Consumer<String>
    ): Boolean {
        val sanitized = sanitizeBodies(rawBody, currentHtml, currentPlain) ?: return false
        var changed = false
        if (sanitized.detailHtml != null && sanitized.detailHtml != currentHtml) {
            htmlSetter.accept(sanitized.detailHtml)
            changed = true
        }
        if (sanitized.plainText != null && sanitized.plainText != currentPlain) {
            plainSetter.accept(sanitized.plainText)
            changed = true
        }
        return changed
    }

    private fun sanitizeBodies(raw: String?, html: String?, plain: String?): SanitizedBodies? {
        val source = firstNonBlank(raw, html, plain)
        if (source.isNullOrBlank()) {
            return null
        }
        val detailHtml = EmailHtmlSanitizer.sanitizeDetailHtml(source)
        val previewSource = detailHtml ?: source
        val previewHtml = EmailHtmlSanitizer.sanitizePreviewHtml(previewSource)
        val plainText = EmailHtmlSanitizer.htmlToPlainText(previewHtml ?: previewSource)
        if (detailHtml.isNullOrBlank() && plainText.isNullOrBlank()) {
            return null
        }
        return SanitizedBodies(
            detailHtml = detailHtml ?: previewSource,
            plainText = plainText?.trim()
        )
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private data class SanitizedBodies(
        val detailHtml: String?,
        val plainText: String?
    )

    companion object {
        private val log = LoggerFactory.getLogger(EmailHtmlBackfillRunner::class.java)

        @Suppress("UNCHECKED_CAST")
        private fun <T> prop(target: Any, property: String): T? {
            val suffix = property.replaceFirstChar { it.uppercaseChar() }
            val getter = listOf("get$suffix", "is$suffix").firstNotNullOfOrNull { name ->
                target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            } ?: return null
            return getter.invoke(target) as T?
        }

        private fun setProp(target: Any, property: String, value: Any?) {
            val suffix = property.replaceFirstChar { it.uppercaseChar() }
            val setter = target.javaClass.methods.firstOrNull { it.name == "set$suffix" && it.parameterCount == 1 }
                ?: return
            setter.invoke(target, value)
        }
    }
}
