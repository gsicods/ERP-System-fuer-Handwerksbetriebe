package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.LieferantReklamation
import org.example.kalkulationsprogramm.domain.ReklamationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LieferantReklamationRepository : JpaRepository<LieferantReklamation, Long> {
    fun findByLieferantIdOrderByStatusAscErstelltAmDesc(lieferantId: Long?): List<LieferantReklamation>

    fun findByStatusOrderByErstelltAmDesc(status: ReklamationStatus): List<LieferantReklamation>
}
