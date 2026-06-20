package org.example.kalkulationsprogramm.repository

import java.time.LocalDateTime
import org.example.kalkulationsprogramm.domain.ProjektNotiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjektNotizRepository : JpaRepository<ProjektNotiz, Long> {
    fun findByProjektIdOrderByErstelltAmDesc(projektId: Long?): List<ProjektNotiz>

    fun findByErstelltAmAfterOrderByErstelltAmDesc(after: LocalDateTime): List<ProjektNotiz>
}
