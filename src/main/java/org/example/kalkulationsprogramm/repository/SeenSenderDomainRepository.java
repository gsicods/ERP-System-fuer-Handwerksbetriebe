package org.example.kalkulationsprogramm.repository;

import java.time.LocalDateTime;

import org.example.kalkulationsprogramm.domain.SeenSenderDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SeenSenderDomainRepository extends JpaRepository<SeenSenderDomain, String> {

    boolean existsByDomain(String domain);

    /**
     * Markiert eine Domain als "schon gesehen". Beim ersten Auftreten wird ein
     * Eintrag mit emailCount=1 angelegt; bei spaeteren Mails wird nur der
     * Zaehler erhoeht (first_seen bleibt unveraendert). MySQL-Upsert vermeidet
     * einen Race zwischen existsByDomain-Check und Insert bei parallelen Imports.
     *
     * Laeuft in eigener Tx (REQUIRES_NEW): Ein DB-Fehler hier darf NICHT die
     * umgebende Mail-Import-Tx (postProcessEmail) auf rollback-only setzen,
     * sonst scheitert das anschliessende emailRepository.save(email). Das
     * try/catch im Aufrufer faengt die Exception zwar ab, kann aber den
     * rollback-only-Status nicht wieder zuruecknehmen — nur eine separate
     * Tx-Grenze tut das.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = "INSERT INTO seen_sender_domain (domain, first_seen, email_count) "
            + "VALUES (:domain, :firstSeen, 1) "
            + "ON DUPLICATE KEY UPDATE email_count = email_count + 1",
            nativeQuery = true)
    void upsertSeen(@Param("domain") String domain,
                    @Param("firstSeen") LocalDateTime firstSeen);
}
