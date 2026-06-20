package org.example.kalkulationsprogramm.repository

import jakarta.persistence.LockModeType
import java.util.Optional
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentCounter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AusgangsGeschaeftsDokumentCounterRepository : JpaRepository<AusgangsGeschaeftsDokumentCounter, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AusgangsGeschaeftsDokumentCounter c WHERE c.monatKey = :monatKey")
    fun findByMonatKeyForUpdate(monatKey: String): Optional<AusgangsGeschaeftsDokumentCounter>

    fun findByMonatKey(monatKey: String): Optional<AusgangsGeschaeftsDokumentCounter>
}
