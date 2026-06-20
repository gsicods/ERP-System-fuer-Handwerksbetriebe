package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Arbeitszeitart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ArbeitszeitartRepository : JpaRepository<Arbeitszeitart, Long> {
    @Query("SELECT a FROM Arbeitszeitart a WHERE a.aktiv = true ORDER BY a.sortierung, a.bezeichnung")
    fun findAllAktiv(): List<Arbeitszeitart>

    @Query("SELECT a FROM Arbeitszeitart a ORDER BY a.sortierung, a.bezeichnung")
    fun findAllSorted(): List<Arbeitszeitart>
}
