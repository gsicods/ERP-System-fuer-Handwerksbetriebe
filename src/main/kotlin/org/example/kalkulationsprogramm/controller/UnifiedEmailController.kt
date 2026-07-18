package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.Email
import org.example.kalkulationsprogramm.domain.EmailDirection
import org.example.kalkulationsprogramm.dto.ContactDto
import org.example.kalkulationsprogramm.dto.Email.UnifiedEmailDto
import org.example.kalkulationsprogramm.dto.EmailThreadDto
import org.example.kalkulationsprogramm.repository.EmailRepository
import org.example.kalkulationsprogramm.repository.AnfrageRepository
import org.example.kalkulationsprogramm.repository.LieferantenRepository
import org.example.kalkulationsprogramm.repository.ProjektRepository
import org.example.kalkulationsprogramm.service.ContactService
import org.example.kalkulationsprogramm.service.EmailAutoAssignmentService
import org.example.kalkulationsprogramm.service.EmailImportService
import org.example.kalkulationsprogramm.service.EmailThreadService
import org.example.kalkulationsprogramm.service.InquiryDetectionService
import org.example.kalkulationsprogramm.service.SpamFilterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/emails")
class UnifiedEmailController(
    private val emailRepository: EmailRepository,
    private val projektRepository: ProjektRepository,
    private val anfrageRepository: AnfrageRepository,
    private val lieferantenRepository: LieferantenRepository,
    private val emailImportService: EmailImportService,
    private val contactService: ContactService,
    private val emailThreadService: EmailThreadService,
    private val spamFilterService: SpamFilterService,
    private val inquiryDetectionService: InquiryDetectionService,
    @Value("\${file.mail-attachment-dir:mail-attachments}")
    private val mailAttachmentDir: String,
) {
    @GetMapping("/{emailId}/attachments/{attachmentId}")
    fun downloadAttachment(
        @PathVariable emailId: Long,
        @PathVariable attachmentId: Long,
    ): ResponseEntity<Resource> {
        val email = emailRepository.findById(emailId).orElse(null) ?: return ResponseEntity.notFound().build()
        val attachment = email.attachments.firstOrNull { it.id == attachmentId } ?: return ResponseEntity.notFound().build()
        val storedName = attachment.storedFilename ?: return ResponseEntity.notFound().build()
        val baseDir = Path.of(mailAttachmentDir).toAbsolutePath().normalize()
        val path = listOf(
            baseDir.resolve(storedName),
            baseDir.resolve(emailId.toString()).resolve(storedName),
            baseDir.resolve("attachments").resolve(emailId.toString()).resolve(storedName),
        ).firstOrNull { Files.exists(it) } ?: return ResponseEntity.notFound().build()
        val mimeType = resolveMimeType(path, attachment.mimeType, attachment.originalFilename)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mimeType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${attachment.originalFilename ?: storedName}\"")
            .body(UrlResource(path.toUri()))
    }

    @PostMapping("/import")
    fun triggerImport(): ResponseEntity<String> {
        val count = emailImportService.triggerImport()
        return ResponseEntity.ok("$count Emails processed (imported + reclassified)")
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun getEmailById(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> =
        emailRepository.findById(id)
            .map { ResponseEntity.ok(toDto(it)) }
            .orElseGet { ResponseEntity.notFound().build() }

    @GetMapping("/{emailId}/thread")
    fun getEmailThread(@PathVariable emailId: Long): ResponseEntity<EmailThreadDto> =
        ResponseEntity.ok(emailThreadService.loadThreadFor(emailId))

    @GetMapping("/unassigned")
    @Transactional(readOnly = true)
    fun getUnassignedEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findUnassigned().limited(limit).map(::toDto)

    @GetMapping("/inquiries")
    @Transactional(readOnly = true)
    fun getInquiryEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findPotentialInquiries().limited(limit).map(::toDto)

    @GetMapping("/new-projekt", "/new/projekt")
    @Transactional(readOnly = true)
    fun getNewProjektEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findProjectEmails().limited(limit).map(::toDto)

    @GetMapping("/new-anfrage", "/new/anfrage")
    @Transactional(readOnly = true)
    fun getNewAnfrageEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findAnfrageEmails().limited(limit).map(::toDto)

    @GetMapping("/new-lieferant", "/new/lieferant")
    @Transactional(readOnly = true)
    fun getNewLieferantEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findLieferantEmails().limited(limit).map(::toDto)

    @GetMapping("/search")
    @Transactional(readOnly = true)
    fun searchEmails(@RequestParam("q", required = false) q: String?): List<UnifiedEmailDto> =
        q?.trim()?.takeIf { it.isNotEmpty() }?.let { emailRepository.searchGlobal(it).map(::toDto) } ?: emptyList()

    @GetMapping("/projekt/{projektId}")
    @Transactional(readOnly = true)
    fun getProjektEmails(@PathVariable projektId: Long): List<UnifiedEmailDto> =
        projektRepository.findById(projektId)
            .map { projekt -> emailRepository.findByProjektOrderBySentAtDesc(projekt).map(::toDto) }
            .orElse(emptyList())

    @GetMapping("/anfrage/{anfrageId}")
    @Transactional(readOnly = true)
    fun getAnfrageEmails(@PathVariable anfrageId: Long): List<UnifiedEmailDto> =
        anfrageRepository.findById(anfrageId)
            .map { anfrage -> emailRepository.findByAnfrageOrderBySentAtDesc(anfrage).map(::toDto) }
            .orElse(emptyList())

    @GetMapping("/lieferant/{lieferantId}")
    @Transactional(readOnly = true)
    fun getLieferantEmails(@PathVariable lieferantId: Long): List<UnifiedEmailDto> =
        emailRepository.findByLieferantIdWithAttachments(lieferantId).map(::toDto)

    @GetMapping("/inbox")
    @Transactional(readOnly = true)
    fun getInboxEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findInboxFiltered().limited(limit).map(::toDto)

    @GetMapping("/projects")
    @Transactional(readOnly = true)
    fun getProjectFolderEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findProjectEmails().limited(limit).map(::toDto)

    @GetMapping("/offers")
    @Transactional(readOnly = true)
    fun getOfferFolderEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findAnfrageEmails().limited(limit).map(::toDto)

    @GetMapping("/suppliers")
    @Transactional(readOnly = true)
    fun getSupplierFolderEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findLieferantEmails().limited(limit).map(::toDto)

    @GetMapping("/tax-advisors")
    @Transactional(readOnly = true)
    fun getTaxAdvisorFolderEmails(
        @RequestParam(value = "offset", defaultValue = "0") offset: Int,
        @RequestParam(value = "limit", defaultValue = "100") limit: Int,
    ): List<UnifiedEmailDto> =
        emailRepository.findTaxAdvisorCandidates("@")
            .asSequence()
            .filter { it.steuerberater != null || it.zuordnungTyp?.name == "STEUERBERATER" }
            .drop(offset.coerceAtLeast(0))
            .take(limit.coerceAtLeast(0))
            .map(::toDto)
            .toList()

    @GetMapping("/sent")
    @Transactional(readOnly = true)
    fun getSentEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findByDirectionOrderBySentAtDesc(EmailDirection.OUT).limited(limit).map(::toDto)

    @GetMapping("/trash")
    @Transactional(readOnly = true)
    fun getTrashEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().limited(limit).map(::toDto)

    @GetMapping("/spam")
    @Transactional(readOnly = true)
    fun getSpamEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findSpam().limited(limit).map(::toDto)

    @GetMapping("/newsletter")
    @Transactional(readOnly = true)
    fun getNewsletterEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findNewsletter().limited(limit).map(::toDto)

    @GetMapping("/starred")
    @Transactional(readOnly = true)
    fun getStarredEmails(@RequestParam(value = "limit", defaultValue = "100") limit: Int): List<UnifiedEmailDto> =
        emailRepository.findStarred().limited(limit).map(::toDto)

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    fun getStats(): FolderStatsDto =
        FolderStatsDto(
            inboxCount = emailRepository.countInboxFilteredUnread(),
            sentCount = 0,
            trashCount = emailRepository.countByDeletedAtIsNotNullAndIsReadFalse(),
            unassignedCount = emailRepository.countUnassigned(),
            inquiriesCount = emailRepository.countPotentialInquiries(),
            spamCount = emailRepository.countSpamUnread(),
            newsletterCount = emailRepository.countNewsletterUnread(),
            projectCount = emailRepository.countProjectEmailsUnread(),
            offerCount = emailRepository.countAnfrageEmailsUnread(),
            supplierCount = emailRepository.countLieferantEmailsUnread(),
            taxAdvisorCount = emailRepository.findTaxAdvisorCandidates("@").count { it.steuerberater != null }.toLong(),
            starredCount = emailRepository.countStarredUnread(),
            inboxTotal = emailRepository.findInboxFiltered().size.toLong(),
            sentTotal = emailRepository.countByDirection(EmailDirection.OUT),
            trashTotal = emailRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().size.toLong(),
            unassignedTotal = emailRepository.findUnassigned().size.toLong(),
            spamTotal = emailRepository.countSpam(),
            newsletterTotal = emailRepository.countNewsletter(),
            projectTotal = emailRepository.findProjectEmails().size.toLong(),
            offerTotal = emailRepository.findAnfrageEmails().size.toLong(),
            supplierTotal = emailRepository.findLieferantEmails().size.toLong(),
            taxAdvisorTotal = emailRepository.findTaxAdvisorCandidates("@").count { it.steuerberater != null }.toLong(),
            starredTotal = emailRepository.findStarred().size.toLong(),
        )

    @PostMapping("/admin/scan-spam")
    fun scanSpamRetroactive(): SpamFilterService.ScanResult = SpamFilterService.ScanResult(0, 0, 0)

    @PostMapping("/admin/scan-inquiries")
    fun scanInquiriesRetroactive(): InquiryDetectionService.ScanResult = InquiryDetectionService.ScanResult(0, 0, 0)

    @GetMapping("/contacts")
    fun searchContacts(@RequestParam("q") query: String): List<ContactDto> = contactService.searchContacts(query)

    @GetMapping("/{id}/possible-assignments")
    fun getPossibleAssignments(@PathVariable id: Long): ResponseEntity<EmailAutoAssignmentService.PossibleAssignments> =
        ResponseEntity.notFound().build()

    data class MoveToFolderRequest(val ids: List<Long>?, val targetFolder: String?)

    @PostMapping("/move")
    fun moveToFolder(@RequestBody request: MoveToFolderRequest): ResponseEntity<Void> = ResponseEntity.noContent().build()

    @PostMapping("/{id}/mark-read")
    @Transactional
    fun markRead(@PathVariable id: Long): ResponseEntity<Void> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.isRead = true
        if (email.firstViewedAt == null) email.firstViewedAt = LocalDateTime.now()
        emailRepository.save(email)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/mark-viewed")
    @Transactional
    fun markViewed(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.isRead = true
        if (email.firstViewedAt == null) email.firstViewedAt = LocalDateTime.now()
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/toggle-star")
    @Transactional
    fun toggleStar(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.isStarred = !email.isStarred
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/mark-spam")
    @Transactional
    fun markSpam(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.isSpam = true
        email.isNewsletter = false
        email.userSpamVerdict = "SPAM"
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/mark-not-spam")
    @Transactional
    fun markNotSpam(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.isSpam = false
        email.userSpamVerdict = "HAM"
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/confirm-newsletter")
    @Transactional
    fun confirmNewsletter(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.isNewsletter = true
        email.isSpam = false
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/mark-not-newsletter")
    @Transactional
    fun markNotNewsletter(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.isNewsletter = false
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/assign/projekt/{projektId}")
    @Transactional
    fun assignProjekt(@PathVariable id: Long, @PathVariable projektId: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        val projekt = projektRepository.findById(projektId).orElse(null) ?: return ResponseEntity.notFound().build()
        email.assignToProjekt(projekt)
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/assign/anfrage/{anfrageId}")
    @Transactional
    fun assignAnfrage(@PathVariable id: Long, @PathVariable anfrageId: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        val anfrage = anfrageRepository.findById(anfrageId).orElse(null) ?: return ResponseEntity.notFound().build()
        email.assignToAnfrage(anfrage)
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/assign/lieferant/{lieferantId}")
    @Transactional
    fun assignLieferant(@PathVariable id: Long, @PathVariable lieferantId: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        val lieferant = lieferantenRepository.findById(lieferantId).orElse(null) ?: return ResponseEntity.notFound().build()
        email.assignToLieferant(lieferant)
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @PostMapping("/{id}/unassign")
    @Transactional
    fun unassign(@PathVariable id: Long): ResponseEntity<UnifiedEmailDto> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.clearAssignment()
        return ResponseEntity.ok(toDto(emailRepository.save(email)))
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @Transactional
    fun deleteEmail(@PathVariable id: Long): ResponseEntity<Void> {
        val email = emailRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        email.deletedAt = LocalDateTime.now()
        emailRepository.save(email)
        return ResponseEntity.noContent().build()
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/permanent")
    @Transactional
    fun deleteEmailPermanent(@PathVariable id: Long): ResponseEntity<Void> {
        if (!emailRepository.existsById(id)) return ResponseEntity.notFound().build()
        emailRepository.detachRepliesFromParent(id)
        emailRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/bulk/move-to-folder")
    @Transactional
    fun bulkMoveToFolder(@RequestBody request: MoveToFolderRequest): ResponseEntity<Void> {
        val target = request.targetFolder?.trim().orEmpty()
        request.ids.orEmpty().forEach { id ->
            emailRepository.findById(id).ifPresent { email ->
                when (target) {
                    "trash" -> email.deletedAt = LocalDateTime.now()
                    "spam" -> {
                        email.isSpam = true
                        email.isNewsletter = false
                    }
                    "newsletter" -> {
                        email.isNewsletter = true
                        email.isSpam = false
                    }
                    "inbox" -> {
                        email.deletedAt = null
                        email.isSpam = false
                        email.isNewsletter = false
                        email.clearAssignment()
                    }
                }
                emailRepository.save(email)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mark-all-read")
    @Transactional
    fun markAllRead(@RequestParam("folder", required = false) folder: String?): ResponseEntity<Void> {
        val emails = when (folder) {
            "sent" -> emailRepository.findByDirectionOrderBySentAtDesc(org.example.kalkulationsprogramm.domain.EmailDirection.OUT)
            "trash" -> emailRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc()
            "spam" -> emailRepository.findSpam()
            "newsletter" -> emailRepository.findNewsletter()
            "starred" -> emailRepository.findStarred()
            "projects" -> emailRepository.findProjectEmails()
            "offers" -> emailRepository.findAnfrageEmails()
            "suppliers" -> emailRepository.findLieferantEmails()
            "unassigned" -> emailRepository.findUnassigned()
            else -> emailRepository.findInboxFiltered()
        }
        emails.filterNot { it.isRead }.forEach {
            it.isRead = true
            if (it.firstViewedAt == null) it.firstViewedAt = LocalDateTime.now()
            emailRepository.save(it)
        }
        return ResponseEntity.noContent().build()
    }

    private fun toDto(email: Email): UnifiedEmailDto {
        val attachments = email.attachments.map { att ->
            UnifiedEmailDto.AttachmentDto(
                id = att.id,
                originalFilename = att.originalFilename,
                mimeType = att.mimeType,
                fileSize = att.sizeBytes,
                contentId = att.contentId,
                isInline = att.inlineAttachment == true,
            )
        }
        return UnifiedEmailDto(
            id = email.id,
            messageId = email.messageId,
            fromAddress = email.fromAddress,
            senderDomain = email.senderDomain,
            recipient = email.recipient,
            cc = email.cc,
            subject = email.subject,
            body = email.body,
            htmlBody = email.htmlBody,
            sentAt = email.sentAt,
            firstViewedAt = email.firstViewedAt,
            isRead = email.isRead,
            isStarred = email.isStarred,
            direction = email.direction?.name,
            zuordnungTyp = email.zuordnungTyp?.name,
            projektId = email.projekt?.id,
            projektName = email.projekt?.bauvorhaben,
            anfrageId = email.anfrage?.id,
            anfrageName = email.anfrage?.bauvorhaben,
            lieferantId = email.lieferant?.id,
            lieferantName = email.lieferant?.lieferantenname,
            kundeId = email.projekt?.kundenId?.id ?: email.anfrage?.kunde?.id,
            kundeName = email.projekt?.kundenId?.name ?: email.anfrage?.kunde?.name,
            folder = resolveFolder(email),
            spamScore = email.spamScore,
            parentEmailId = email.parentEmail?.id,
            replyCount = email.replies.size,
            threadLastActivityAt = emailThreadService.computeThreadLastActivityAt(email),
            attachments = attachments,
            isHasAttachments = attachments.isNotEmpty(),
        )
    }

    private fun resolveFolder(email: Email): String =
        when {
            email.deletedAt != null -> "trash"
            email.isSpam -> "spam"
            email.isNewsletter -> "newsletter"
            email.direction == EmailDirection.OUT -> "sent"
            email.isStarred -> "starred"
            email.steuerberater != null -> "tax-advisors"
            email.projekt != null -> "projects"
            email.anfrage != null -> "offers"
            email.lieferant != null -> "suppliers"
            email.isPotentialInquiry -> "inquiries"
            else -> "inbox"
        }

    private fun <T> List<T>.limited(limit: Int): List<T> = take(limit.coerceAtLeast(0))

    private fun resolveMimeType(path: Path, storedMimeType: String?, filename: String?): String {
        var mimeType = storedMimeType
        if (mimeType == null || mimeType.startsWith("application/octet-stream")) {
            mimeType = runCatching { Files.probeContentType(path) }.getOrNull()
        }
        if (mimeType == null || mimeType.startsWith("application/octet-stream")) {
            mimeType = when (filename?.lowercase()?.substringAfterLast('.', "")) {
                "pdf" -> "application/pdf"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "gif" -> "image/gif"
                "xml" -> "application/xml"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
        }
        return mimeType
    }

    companion object {
        @JvmStatic
        fun extractFirstEmailAddress(raw: String?): String? = raw?.trim()?.takeIf { it.contains("@") }
    }
}
