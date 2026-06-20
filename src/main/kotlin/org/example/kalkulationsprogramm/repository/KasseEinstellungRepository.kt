package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.KasseEinstellung
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface KasseEinstellungRepository : JpaRepository<KasseEinstellung, Long> {
    @Query("SELECT k FROM KasseEinstellung k ORDER BY k.id ASC")
    fun findSingleton(): Optional<KasseEinstellung>
}
