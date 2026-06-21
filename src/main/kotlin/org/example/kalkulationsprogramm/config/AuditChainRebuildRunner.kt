package org.example.kalkulationsprogramm.config

import org.example.kalkulationsprogramm.service.AuditChainRepairService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

@Configuration
@ConditionalOnProperty(name = ["audit.chain.rebuild-on-start"], havingValue = "true")
class AuditChainRebuildRunner(
    private val repairService: AuditChainRepairService
) {
    @Bean
    @Order(20)
    fun auditChainRebuild(): ApplicationRunner = ApplicationRunner {
        log.warn(
            "audit.chain.rebuild-on-start=true gesetzt - Audit-Hash-Kette wird einmalig neu aufgebaut. Flag nach diesem Start wieder entfernen!"
        )
        val anzahl = repairService.rebuildChain()
        log.warn(
            "Audit-Hash-Kette neu aufgebaut ({} Eintraege). Bitte audit.chain.rebuild-on-start jetzt wieder entfernen.",
            anzahl
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuditChainRebuildRunner::class.java)
    }
}
