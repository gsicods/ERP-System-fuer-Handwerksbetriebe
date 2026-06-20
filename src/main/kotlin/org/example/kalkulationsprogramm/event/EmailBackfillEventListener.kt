package org.example.kalkulationsprogramm.event

import org.example.kalkulationsprogramm.domain.Email
import org.example.kalkulationsprogramm.domain.Kunde
import org.example.kalkulationsprogramm.repository.AnfrageRepository
import org.example.kalkulationsprogramm.repository.EmailRepository
import org.example.kalkulationsprogramm.repository.KundeRepository
import org.example.kalkulationsprogramm.repository.LieferantenRepository
import org.example.kalkulationsprogramm.repository.ProjektRepository
import org.example.kalkulationsprogramm.service.EmailAttachmentProcessingService
import org.example.kalkulationsprogramm.service.EmailAutoAssignmentService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EmailBackfillEventListener(
    private val kundeRepository: KundeRepository,
    private val lieferantenRepository: LieferantenRepository,
    private val anfrageRepository: AnfrageRepository,
    private val projektRepository: ProjektRepository,
    private val emailRepository: EmailRepository,
    private val emailAttachmentProcessingService: EmailAttachmentProcessingService,
    private val emailAutoAssignmentService: EmailAutoAssignmentService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleEmailAddressChanged(event: EmailAddressChangedEvent) {
        val newAddresses = event.newAddresses
        if (newAddresses == null || newAddresses.isEmpty()) {
            log.debug("[EmailBackfill] Keine neuen Adressen fuer {} ID={}", event.entityType, event.entityId)
            return
        }

        log.info(
            "[EmailBackfill] Starte Backfill fuer {} ID={} mit {} neuen Adressen",
            event.entityType,
            event.entityId,
            newAddresses.size,
        )

        try {
            val count = when (event.entityType) {
                EmailAddressChangedEvent.EntityType.KUNDE -> handleKundeBackfill(event)
                EmailAddressChangedEvent.EntityType.LIEFERANT -> handleLieferantBackfill(event)
                EmailAddressChangedEvent.EntityType.ANFRAGE -> handleAnfrageBackfill(event)
                EmailAddressChangedEvent.EntityType.PROJEKT -> handleProjektBackfill(event)
                EmailAddressChangedEvent.EntityType.ANGEBOT -> 0
            }

            log.info(
                "[EmailBackfill] {} ID={}: {} E-Mails rueckwirkend zugeordnet",
                event.entityType,
                event.entityId,
                count,
            )
        } catch (e: Exception) {
            log.error("[EmailBackfill] Fehler bei {} ID={}: {}", event.entityType, event.entityId, e.message, e)
        }
    }

    private fun handleKundeBackfill(event: EmailAddressChangedEvent): Int {
        val kunde = kundeRepository.findById(event.entityId).orElse(null) ?: return 0
        var count = 0
        event.newAddresses.orEmpty().forEach { address ->
            emailRepository.findUnassignedByAddress(address).forEach { email ->
                if (tryAssignToKundeContext(email, kunde)) {
                    count++
                }
            }
        }
        return count
    }

    private fun tryAssignToKundeContext(email: Email, kunde: Kunde): Boolean {
        val kundeId = getLongProperty(kunde, "getId")
        val projekte = projektRepository.findByKundenId_Id(kundeId)
        val anfragen = anfrageRepository.findByKundeId(kundeId)
        val total = projekte.size + anfragen.size

        if (total == 1) {
            if (projekte.isNotEmpty()) {
                email.assignToProjekt(projekte.first())
                log.info(
                    "[EmailBackfill] Auto-assigned email {} to single project {} of customer {}",
                    getLongProperty(email, "getId"),
                    getLongProperty(projekte.first(), "getId"),
                    kundeId,
                )
            } else {
                email.assignToAnfrage(anfragen.first())
                log.info(
                    "[EmailBackfill] Auto-assigned email {} to single offer {} of customer {}",
                    getLongProperty(email, "getId"),
                    getLongProperty(anfragen.first(), "getId"),
                    kundeId,
                )
            }
            emailRepository.save(email)
            return true
        }

        return emailAutoAssignmentService.tryAssignByKeywords(email, projekte, anfragen)
    }

    private fun handleLieferantBackfill(event: EmailAddressChangedEvent): Int {
        val lieferant = lieferantenRepository.findById(event.entityId).orElse(null)
        if (lieferant == null) {
            log.warn("[EmailBackfill] Lieferant ID={} nicht gefunden", event.entityId)
            return 0
        }

        var count = 0
        var attachmentErrors = 0
        event.newAddresses.orEmpty().forEach { address ->
            val domain = extractDomain(address)
            log.info("[EmailBackfill] Lieferant ID={}: Pruefe Domain '{}' (aus Adresse '{}')", event.entityId, domain, address)

            if (domain != null) {
                val emails = emailRepository.findUnassignedByDomain(domain)
                log.info("[EmailBackfill] Gefunden: {} unzugeordnete Emails fuer Domain '{}'", emails.size, domain)

                emails.forEach { email ->
                    email.assignToLieferant(lieferant)
                    setBooleanProperty(email, "setPotentialInquiry", false)
                    setIntProperty(email, "setInquiryScore", 0)
                    emailRepository.saveAndFlush(email)
                    log.info("[EmailBackfill] Email {} dem Lieferant {} zugeordnet", getLongProperty(email, "getId"), getLongProperty(lieferant, "getId"))

                    try {
                        log.info("[EmailBackfill] Verarbeite Anhaenge fuer Email {} sequentiell...", getLongProperty(email, "getId"))
                        val processed = emailAttachmentProcessingService.processLieferantAttachments(email)
                        log.info("[EmailBackfill] Anhaenge fuer Email {} erfolgreich verarbeitet: {} Dokumente erstellt", getLongProperty(email, "getId"), processed)
                    } catch (e: Exception) {
                        log.error("[EmailBackfill] Error processing attachments for email {}: {}", getLongProperty(email, "getId"), e.message)
                        attachmentErrors++
                    }

                    count++
                }

                emailRepository.findInquiriesByDomain(domain).forEach { email ->
                    setBooleanProperty(email, "setPotentialInquiry", false)
                    setIntProperty(email, "setInquiryScore", 0)
                    emailRepository.saveAndFlush(email)
                    log.info("[EmailBackfill] Inquiry-Flag entfernt fuer Email {} (Lieferant-Domain {})", getLongProperty(email, "getId"), domain)
                }
            }
        }

        val alreadyAssignedEmails = emailRepository.findByLieferantIdWithAttachments(event.entityId)
        var alreadyProcessed = 0
        log.info("[EmailBackfill] Pruefe {} bereits zugeordnete Emails auf unverarbeitete Anhaenge", alreadyAssignedEmails.size)

        alreadyAssignedEmails.forEach { email ->
            val attachments = getCollectionProperty(email, "getAttachments")
            if (attachments.isNullOrEmpty()) {
                return@forEach
            }

            val hasUnprocessedAttachments = attachments.any { attachment ->
                getBooleanProperty(attachment, "getAiProcessed") != true &&
                    getStringProperty(attachment, "getOriginalFilename") != null &&
                    getStringProperty(attachment, "getOriginalFilename")!!.lowercase().endsWith(".pdf")
            }

            if (hasUnprocessedAttachments) {
                try {
                    log.info("[EmailBackfill] Bereits zugeordnete Email {} hat unverarbeitete Anhaenge - verarbeite...", getLongProperty(email, "getId"))
                    val processed = emailAttachmentProcessingService.processLieferantAttachments(email)
                    if (processed > 0) {
                        alreadyProcessed += processed
                        log.info("[EmailBackfill] Email {}: {} Dokumente aus Anhaengen erstellt", getLongProperty(email, "getId"), processed)
                    }
                } catch (e: Exception) {
                    log.error("[EmailBackfill] Fehler bei Anhang-Verarbeitung fuer bereits zugeordnete Email {}: {}", getLongProperty(email, "getId"), e.message)
                    attachmentErrors++
                }
            }
        }

        if (alreadyProcessed > 0) {
            log.info("[EmailBackfill] {} Dokumente aus bereits zugeordneten Emails erstellt", alreadyProcessed)
        }

        if (attachmentErrors > 0) {
            log.warn("[EmailBackfill] Lieferant {}: {} E-Mails hatten Fehler bei Anhang-Verarbeitung", event.entityId, attachmentErrors)
        }

        return count + alreadyProcessed
    }

    private fun handleAnfrageBackfill(event: EmailAddressChangedEvent): Int {
        val anfrage = anfrageRepository.findById(event.entityId).orElse(null) ?: return 0
        var count = 0
        event.newAddresses.orEmpty().forEach { address ->
            emailRepository.findUnassignedByAddress(address).forEach { email ->
                email.assignToAnfrage(anfrage)
                emailRepository.save(email)
                count++
            }
        }
        return count
    }

    private fun handleProjektBackfill(event: EmailAddressChangedEvent): Int {
        val projekt = projektRepository.findById(event.entityId).orElse(null) ?: return 0
        var count = 0
        event.newAddresses.orEmpty().forEach { address ->
            emailRepository.findUnassignedByAddress(address).forEach { email ->
                email.assignToProjekt(projekt)
                emailRepository.save(email)
                count++
            }
        }
        return count
    }

    private fun extractDomain(email: String?): String? =
        email?.takeIf { it.contains("@") }?.substringAfterLast("@")?.lowercase()

    private fun getLongProperty(target: Any, methodName: String): Long? =
        target.javaClass.getMethod(methodName).invoke(target) as Long?

    private fun getBooleanProperty(target: Any, methodName: String): Boolean? =
        target.javaClass.getMethod(methodName).invoke(target) as Boolean?

    private fun getStringProperty(target: Any, methodName: String): String? =
        target.javaClass.getMethod(methodName).invoke(target) as String?

    @Suppress("UNCHECKED_CAST")
    private fun getCollectionProperty(target: Any, methodName: String): Collection<Any> =
        target.javaClass.getMethod(methodName).invoke(target) as? Collection<Any> ?: emptyList()

    private fun setBooleanProperty(target: Any, methodName: String, value: Boolean) {
        target.javaClass.getMethod(methodName, Boolean::class.javaPrimitiveType).invoke(target, value)
    }

    private fun setIntProperty(target: Any, methodName: String, value: Int) {
        target.javaClass.getMethod(methodName, Int::class.javaPrimitiveType).invoke(target, value)
    }
}
