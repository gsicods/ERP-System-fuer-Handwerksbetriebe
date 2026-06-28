package org.example.kalkulationsprogramm.config

import org.example.kalkulationsprogramm.domain.AuditChainState
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentAudit
import org.example.kalkulationsprogramm.repository.AuditChainStateRepository
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentAuditRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Configuration
class AuditChainBackfillRunner(
    private val auditRepository: AusgangsGeschaeftsDokumentAuditRepository,
    private val chainStateRepository: AuditChainStateRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    @Bean
    @Order(10)
    fun auditChainBackfill(): ApplicationRunner =
        ApplicationRunner { transactionTemplate.executeWithoutResult { backfill() } }

    @Transactional
    fun backfill() {
        val nichtVerkettet = auditRepository.findByChainIndexIsNullOrderByGeaendertAmAscIdAsc()

        if (nichtVerkettet.isEmpty()) {
            log.debug("Audit-Hash-Kette: keine offenen Einträge zum Backfill.")
            return
        }

        var state = chainStateRepository.lockState()
        if (state == null) {
            state = AuditChainState()
            writeField(state, "id", 1)
            writeField(state, "lastChainIndex", -1L)
            writeField(state, "lastEntryHash", null)
            writeField(state, "updatedAt", LocalDateTime.now())
            state = chainStateRepository.saveAndFlush(state)
        }

        var index = readLong(state, "lastChainIndex") ?: -1L
        var previousHash = readString(state, "lastEntryHash")

        log.info(
            "Audit-Hash-Kette: starte Backfill für {} Einträge ab chain_index={}",
            nichtVerkettet.size,
            index + 1,
        )

        for (audit in nichtVerkettet) {
            index++
            writeField(audit, "chainIndex", index)
            writeField(audit, "previousHash", previousHash)
            val entryHash = audit.computeEntryHash()
            writeField(audit, "entryHash", entryHash)
            previousHash = entryHash
        }

        auditRepository.saveAll(nichtVerkettet)

        writeField(state, "lastChainIndex", index)
        writeField(state, "lastEntryHash", previousHash)
        writeField(state, "updatedAt", LocalDateTime.now())
        chainStateRepository.saveAndFlush(state)

        log.info(
            "Audit-Hash-Kette: Backfill abgeschlossen, neuer Kopf chain_index={} entry_hash={}",
            index,
            previousHash,
        )
    }

    private fun readString(target: Any?, fieldName: String): String? =
        readField(target, fieldName) as? String

    private fun readLong(target: Any?, fieldName: String): Long? =
        (readField(target, fieldName) as? Number)?.toLong()

    private fun readField(target: Any?, fieldName: String): Any? {
        if (target == null) return null
        var type: Class<*>? = target.javaClass
        while (type != null) {
            try {
                val field = type.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(target)
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            }
        }
        return null
    }

    private fun writeField(target: Any, fieldName: String, value: Any?) {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            try {
                val field = type.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(target, value)
                return
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuditChainBackfillRunner::class.java)
    }
}
