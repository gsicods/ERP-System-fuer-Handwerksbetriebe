package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.AnfrageNotiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnfrageNotizRepository : JpaRepository<AnfrageNotiz, Long> {
    fun findByAnfrageIdOrderByErstelltAmDesc(anfrageId: Long?): List<AnfrageNotiz>
}
