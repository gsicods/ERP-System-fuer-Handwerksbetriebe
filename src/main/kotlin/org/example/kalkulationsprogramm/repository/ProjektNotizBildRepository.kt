package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.ProjektNotizBild
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjektNotizBildRepository : JpaRepository<ProjektNotizBild, Long> {
    fun findByNotizId(notizId: Long?): List<ProjektNotizBild>

    fun deleteByNotizId(notizId: Long?)
}
