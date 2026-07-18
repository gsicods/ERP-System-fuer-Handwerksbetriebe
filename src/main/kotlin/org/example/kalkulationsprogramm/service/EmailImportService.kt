package org.example.kalkulationsprogramm.service

import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Address
import jakarta.mail.BodyPart
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeUtility
import org.example.kalkulationsprogramm.domain.Email
import org.example.kalkulationsprogramm.domain.EmailAttachment
import org.example.kalkulationsprogramm.domain.EmailDirection
import org.example.kalkulationsprogramm.repository.EmailBlacklistRepository
import org.example.kalkulationsprogramm.repository.EmailRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties
import java.util.UUID

@Service
open class EmailImportService(
    private val emailRepository: EmailRepository,
    private val emailAutoAssignmentService: EmailAutoAssignmentService,
    private val emailAttachmentProcessingService: EmailAttachmentProcessingService,
    private val spamFilterService: SpamFilterService,
    private val inquiryDetectionService: InquiryDetectionService,
    private val steuerberaterEmailProcessingService: SteuerberaterEmailProcessingService,
    private val systemSettingsService: SystemSettingsService,
    private val outOfOfficeResponder: OutOfOfficeResponder,
    private val emailBlacklistRepository: EmailBlacklistRepository,
    @Value("\${file.mail-attachment-dir:mail-attachments}")
    private val mailAttachmentDir: String,
    @Value("\${email.features.enabled:true}")
    private val emailFeaturesEnabled: Boolean = true,
) {
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    fun importNewEmails() {
        if (!emailFeaturesEnabled || !systemSettingsService.isImapConfigured) return
        runCatching { doImport() }
            .onFailure { log.error("[EmailImport] Fehler beim Import: {}", it.message, it) }
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    fun deactivateExpiredOutOfOffice() {
        if (!emailFeaturesEnabled) return
        runCatching { outOfOfficeResponder.deactivateExpiredSchedules() }
            .onFailure { log.warn("[OOO] Auto-Deaktivierung fehlgeschlagen: {}", it.message) }
    }

    fun doImport(): Int {
        if (!systemSettingsService.isImapConfigured) {
            log.debug("[EmailImport] IMAP nicht konfiguriert")
            return 0
        }
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.mime.address.strict", "false")
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.timeout", "30000")
        }
        val session = Session.getInstance(props)
        var imported = 0
        try {
            session.getStore("imaps").use { store ->
                store.connect(
                    systemSettingsService.imapHost,
                    systemSettingsService.imapPort,
                    systemSettingsService.imapUsername,
                    systemSettingsService.imapPassword,
                )
                INCOMING_FOLDERS.forEach { imported += importFromFolder(store, it, EmailDirection.IN) }
                OUTGOING_FOLDERS.forEach { imported += importFromFolder(store, it, EmailDirection.OUT) }
            }
        } catch (e: MessagingException) {
            log.error("[EmailImport] IMAP-Fehler: {}", e.message)
        }
        return imported
    }

    fun triggerImport(): Int = doImport()

    private fun importFromFolder(store: Store, folderName: String, direction: EmailDirection): Int {
        val genericFolder = runCatching { store.getFolder(folderName) }.getOrNull() ?: return 0
        val folder = genericFolder as? IMAPFolder ?: return 0
        if (!folder.exists()) return 0
        var imported = 0
        folder.open(Folder.READ_ONLY)
        try {
            folder.messages.forEach { message ->
                runCatching {
                    if (importMessage(message, folder, direction)) imported++
                }.onFailure {
                    log.warn(
                        "[EmailImport] Nachricht in Ordner '{}' konnte nicht importiert werden: {}",
                        folderName,
                        it.message,
                    )
                }
            }
        } finally {
            runCatching { folder.close(false) }
        }
        if (imported > 0) {
            log.info("[EmailImport] Ordner {}: {} E-Mail(s) importiert", folderName, imported)
        }
        return imported
    }

    @Transactional
    open fun importMessage(msg: Message, folder: IMAPFolder, direction: EmailDirection): Boolean {
        val messageId = extractMessageId(msg, folder)
        if (emailRepository.existsByMessageId(messageId)) return false
        val fromAddress = formatAddresses(msg.from)
        if (isBlacklisted(fromAddress)) return false

        val content = extractContent(msg)
        val email = Email().apply {
            this.messageId = messageId
            this.fromAddress = fromAddress
            this.senderDomain = extractDomain(fromAddress)
            this.recipient = formatAddresses(msg.getRecipients(Message.RecipientType.TO))
            this.cc = formatAddresses(msg.getRecipients(Message.RecipientType.CC))
            this.replyToAddress = formatAddresses(msg.replyTo)
            this.subject = decodeHeader(msg.subject)
            this.body = content.text
            this.htmlBody = content.html
            this.rawBody = content.text ?: content.html
            this.sentAt = msg.sentDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: LocalDateTime.now()
            this.direction = direction
            this.imapFolder = folder.fullName
            this.imapUid = runCatching { folder.getUID(msg) }.getOrNull()
            this.isRead = msg.flags?.contains(Flags.Flag.SEEN) == true
        }

        content.attachments.forEach(email::addAttachment)
        spamFilterService.analyzeAndMarkSpam(email)
        inquiryDetectionService.analyzeAndMarkInquiry(email)
        val saved = emailRepository.save(email)

        runCatching { emailAutoAssignmentService.tryAutoAssign(saved) }
            .onFailure { log.debug("[EmailImport] Auto-Zuordnung fehlgeschlagen fuer {}: {}", saved.id, it.message) }
        runCatching { emailAttachmentProcessingService.processLieferantAttachments(saved) }
            .onFailure { log.debug("[EmailImport] Lieferanten-Anhangverarbeitung fehlgeschlagen fuer {}: {}", saved.id, it.message) }
        runCatching { steuerberaterEmailProcessingService.processSteuerberaterEmail(saved) }
            .onFailure { log.debug("[EmailImport] Steuerberater-Verarbeitung fehlgeschlagen fuer {}: {}", saved.id, it.message) }
        runCatching { postProcessEmail(saved) }
        return true
    }

    fun backfillAttachmentFilenames(): Int = 0

    fun postProcessEmail(email: Email) {
        log.debug("Post-processing email {}", email.id)
    }

    fun reprocessSpam(): Int {
        var processed = 0
        emailRepository.findUnanalyzedForSpam().forEach { email ->
            spamFilterService.analyzeAndMarkSpam(email)
            emailRepository.save(email)
            processed++
        }
        return processed
    }

    fun getStats(): Map<String, Long> =
        mapOf(
            "inbox" to emailRepository.countByDirection(EmailDirection.IN),
            "sent" to emailRepository.countByDirection(EmailDirection.OUT),
            "spam" to emailRepository.countSpam(),
            "newsletter" to emailRepository.countNewsletter(),
        )

    fun backfillSteuerberaterEmails(): Int {
        var processed = 0
        emailRepository.findTaxAdvisorCandidates("@").forEach { email ->
            if (steuerberaterEmailProcessingService.processSteuerberaterEmail(email)) processed++
        }
        return processed
    }

    fun backfillParentEmails(): Int = 0

    fun deleteEmailFromServer(email: Email) {
        log.debug("Server-side delete not implemented for email {}", email.id)
    }

    private fun extractContent(part: Part): ExtractedContent {
        val result = ExtractedContent()
        extractContent(part, result)
        return result
    }

    private fun extractContent(part: Part, result: ExtractedContent) {
        when {
            part.isMimeType("text/plain") && Part.ATTACHMENT != part.disposition -> {
                if (result.text == null) result.text = part.content as? String
            }
            part.isMimeType("text/html") && Part.ATTACHMENT != part.disposition -> {
                if (result.html == null) result.html = part.content as? String
            }
            part.isMimeType("multipart/*") -> {
                val multipart = part.content as? Multipart ?: return
                for (i in 0 until multipart.count) {
                    extractContent(multipart.getBodyPart(i), result)
                }
            }
            isAttachment(part) -> {
                saveAttachment(part as? BodyPart ?: return)?.let(result.attachments::add)
            }
        }
    }

    private fun isAttachment(part: Part): Boolean =
        Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) ||
            Part.INLINE.equals(part.disposition, ignoreCase = true) ||
            !part.fileName.isNullOrBlank()

    private fun saveAttachment(part: BodyPart): EmailAttachment? {
        val originalName = decodeHeader(part.fileName).takeUnless { it.isNullOrBlank() } ?: "attachment"
        val storedName = "${UUID.randomUUID()}-$originalName".replace(Regex("[\\\\/]+"), "_")
        val dir = Path.of(mailAttachmentDir).toAbsolutePath().normalize()
        Files.createDirectories(dir)
        val path = dir.resolve(storedName)
        (part.inputStream as InputStream).use { Files.copy(it, path) }
        return EmailAttachment().apply {
            originalFilename = originalName
            storedFilename = storedName
            mimeType = part.contentType?.substringBefore(';')
            sizeBytes = runCatching { Files.size(path) }.getOrNull()
            contentId = part.getHeader("Content-ID")?.firstOrNull()?.trim('<', '>')
            inlineAttachment = Part.INLINE.equals(part.disposition, ignoreCase = true)
        }
    }

    private fun extractMessageId(msg: Message, folder: IMAPFolder): String {
        val header = msg.getHeader("Message-ID")?.firstOrNull()?.trim()
        if (!header.isNullOrBlank()) return header
        val uid = runCatching { folder.getUID(msg) }.getOrDefault(-1L)
        return "<${folder.fullName}-$uid@local-import>"
    }

    private fun isBlacklisted(fromAddress: String?): Boolean {
        val address = extractFirstEmailAddress(fromAddress)?.lowercase() ?: return false
        return emailBlacklistRepository.existsByEmailAddress(address)
    }

    private fun extractDomain(fromAddress: String?): String? =
        extractFirstEmailAddress(fromAddress)
            ?.substringAfterLast('@', "")
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()

    private fun formatAddresses(addresses: Array<Address>?): String? =
        addresses?.joinToString(", ") { address ->
            if (address is InternetAddress) {
                address.toUnicodeString()
            } else {
                address.toString()
            }
        }?.takeIf { it.isNotBlank() }

    private fun decodeHeader(value: String?): String? =
        value?.let { runCatching { MimeUtility.decodeText(it) }.getOrDefault(it) }

    private class ExtractedContent {
        var text: String? = null
        var html: String? = null
        val attachments = ArrayList<EmailAttachment>()
    }

    companion object {
        private val log = LoggerFactory.getLogger(EmailImportService::class.java)
        private val INCOMING_FOLDERS = listOf(
            "INBOX",
            "INBOX.Archives (2).Eingangsanfragen",
            "INBOX.Archives (2).Eingangs Ab's",
            "INBOX.Archives (2).Eingangsrechnungen",
            "INBOX.Archives (2).Gedruckte Eingangsrechnungen",
            "INBOX.Archives (2).Werkstoffzeugnisse",
        )
        private val OUTGOING_FOLDERS = listOf("INBOX.Sent", "INBOX.Sent Items")

        @JvmStatic
        fun extractFirstEmailAddress(raw: String?): String? {
            val value = raw?.trim()?.takeIf { it.contains("@") } ?: return null
            val match = Regex("<([^>]+)>").find(value)
            return (match?.groupValues?.getOrNull(1) ?: value).trim()
        }
    }
}
