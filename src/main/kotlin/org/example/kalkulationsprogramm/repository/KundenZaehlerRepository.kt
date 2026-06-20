package org.example.kalkulationsprogramm.repository

import jakarta.persistence.LockModeType
import org.example.kalkulationsprogramm.domain.KundenZaehler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface KundenZaehlerRepository : JpaRepository<KundenZaehler, Int> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM KundenZaehler k WHERE k.id = 1")
    fun lockAndGet(): KundenZaehler
}
