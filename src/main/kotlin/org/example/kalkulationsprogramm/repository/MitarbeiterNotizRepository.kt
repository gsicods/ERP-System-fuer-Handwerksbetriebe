package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.MitarbeiterNotiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MitarbeiterNotizRepository : JpaRepository<MitarbeiterNotiz, Long> {
    fun findByMitarbeiterIdOrderByErstelltAmDesc(mitarbeiterId: Long?): List<MitarbeiterNotiz>
}
