package org.example.kalkulationsprogramm.repository

import java.time.LocalDateTime
import org.example.kalkulationsprogramm.domain.SeenSenderDomain
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Repository
interface SeenSenderDomainRepository : JpaRepository<SeenSenderDomain, String> {
    fun existsByDomain(domain: String): Boolean

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(
        value = "INSERT INTO seen_sender_domain (domain, first_seen, email_count) " +
            "VALUES (:domain, :firstSeen, 1) " +
            "ON DUPLICATE KEY UPDATE email_count = email_count + 1",
        nativeQuery = true,
    )
    fun upsertSeen(
        @Param("domain") domain: String,
        @Param("firstSeen") firstSeen: LocalDateTime,
    )
}
