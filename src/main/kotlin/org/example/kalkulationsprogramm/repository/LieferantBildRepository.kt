package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.LieferantBild
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LieferantBildRepository : JpaRepository<LieferantBild, Long> {
    fun findByLieferantIdOrderByErstelltAmDesc(lieferantId: Long?): List<LieferantBild>

    fun findByGespeicherterDateiname(gespeicherterDateiname: String): Optional<LieferantBild>
}
