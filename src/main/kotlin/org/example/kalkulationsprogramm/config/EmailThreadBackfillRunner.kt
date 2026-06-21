package org.example.kalkulationsprogramm.config

import org.example.kalkulationsprogramm.service.EmailImportService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order

@Configuration
class EmailThreadBackfillRunner(
    private val emailImportService: EmailImportService
) {
    @EventListener(ApplicationReadyEvent::class)
    @Order(100)
    fun backfillEmailThreadsOnStartup() {
        try {
            log.info("[EmailThreadBackfill] Starte Subject-basierten Backfill fuer Thread-Erkennung...")
            val verknuepft = emailImportService.backfillParentEmails()
            if (verknuepft > 0) {
                log.info("[EmailThreadBackfill] {} E-Mails nachtraeglich an Thread-Parent verknuepft.", verknuepft)
            } else {
                log.debug("[EmailThreadBackfill] Keine offenen E-Mails - nichts zu tun.")
            }
        } catch (ex: Exception) {
            log.error("[EmailThreadBackfill] Backfill fehlgeschlagen, ueberspringe.", ex)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(EmailThreadBackfillRunner::class.java)
    }
}
