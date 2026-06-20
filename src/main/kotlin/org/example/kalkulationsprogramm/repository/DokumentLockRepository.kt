package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.DokumentLock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DokumentLockRepository : JpaRepository<DokumentLock, Long> {
    fun findByDokumentTypAndDokumentId(dokumentTyp: String, dokumentId: Long?): Optional<DokumentLock>

    fun deleteByDokumentTypAndDokumentId(dokumentTyp: String, dokumentId: Long?)
}
