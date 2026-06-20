package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.BwaPosition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BwaPositionRepository : JpaRepository<BwaPosition, Long> {
    fun findByBwaUploadIdOrderByKontonummerAsc(bwaUploadId: Long?): List<BwaPosition>

    fun findByBwaUploadIdAndKategorie(bwaUploadId: Long?, kategorie: String): List<BwaPosition>

    fun findByBwaUploadIdAndInRechnungenGefundenFalse(bwaUploadId: Long?): List<BwaPosition>

    @Query("SELECT p.kategorie, SUM(p.betragMonat) FROM BwaPosition p WHERE p.bwaUpload.id = :bwaId GROUP BY p.kategorie")
    fun summenNachKategorie(@Param("bwaId") bwaId: Long?): List<Array<Any>>
}
