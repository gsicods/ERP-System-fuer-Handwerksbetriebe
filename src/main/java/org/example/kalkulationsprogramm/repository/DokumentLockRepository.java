package org.example.kalkulationsprogramm.repository;

import java.util.Optional;

import org.example.kalkulationsprogramm.domain.DokumentLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DokumentLockRepository extends JpaRepository<DokumentLock, Long> {

    Optional<DokumentLock> findByDokumentTypAndDokumentId(String dokumentTyp, Long dokumentId);

    void deleteByDokumentTypAndDokumentId(String dokumentTyp, Long dokumentId);
}
