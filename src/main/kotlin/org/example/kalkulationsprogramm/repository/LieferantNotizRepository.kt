package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.LieferantNotiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LieferantNotizRepository : JpaRepository<LieferantNotiz, Long> {
    fun findByLieferantIdOrderByErstelltAmDesc(lieferantId: Long?): List<LieferantNotiz>

    fun findByLieferantIdAndTextContainingIgnoreCaseOrderByErstelltAmDesc(
        lieferantId: Long?,
        query: String,
    ): List<LieferantNotiz>
}
