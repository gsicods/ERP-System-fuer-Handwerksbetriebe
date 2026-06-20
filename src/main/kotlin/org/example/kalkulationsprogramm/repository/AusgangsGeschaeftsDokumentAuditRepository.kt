package org.example.kalkulationsprogramm.repository

import java.time.LocalDateTime
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentAudit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AusgangsGeschaeftsDokumentAuditRepository : JpaRepository<AusgangsGeschaeftsDokumentAudit, Long> {
    fun findByDokumentIdOrderByGeaendertAmDesc(dokumentId: Long?): List<AusgangsGeschaeftsDokumentAudit>

    fun findByDokumentNummerOrderByGeaendertAmDesc(dokumentNummer: String): List<AusgangsGeschaeftsDokumentAudit>

    fun findByGeaendertAmBetweenOrderByGeaendertAmAsc(
        von: LocalDateTime,
        bis: LocalDateTime,
    ): List<AusgangsGeschaeftsDokumentAudit>

    fun findAllByOrderByChainIndexAsc(): List<AusgangsGeschaeftsDokumentAudit>

    fun findByChainIndexIsNullOrderByGeaendertAmAscIdAsc(): List<AusgangsGeschaeftsDokumentAudit>

    fun findByGeaendertAmBetweenOrderByChainIndexAsc(
        von: LocalDateTime,
        bis: LocalDateTime,
    ): List<AusgangsGeschaeftsDokumentAudit>

    fun countByGeaendertAmBetween(von: LocalDateTime, bis: LocalDateTime): Long
}
