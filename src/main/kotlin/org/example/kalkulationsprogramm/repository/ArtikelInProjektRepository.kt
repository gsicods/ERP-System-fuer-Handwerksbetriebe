package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.ArtikelInProjekt
import org.example.kalkulationsprogramm.dto.Projekt.MaterialKilogrammDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ArtikelInProjektRepository : JpaRepository<ArtikelInProjekt, Long> {
    fun findByBestelltFalseOrderByLieferant_LieferantennameAscProjekt_BauvorhabenAsc(): List<ArtikelInProjekt>

    fun findByBestelltFalseAndLieferant_IdOrderByProjekt_BauvorhabenAsc(lieferantId: Long?): List<ArtikelInProjekt>

    fun findByArtikel_IdAndLieferant_IdAndBestelltFalse(artikelId: Long?, lieferantId: Long?): List<ArtikelInProjekt>

    fun findByProjekt_Id(projektId: Long?): List<ArtikelInProjekt>

    @Query(
        "SELECT new org.example.kalkulationsprogramm.dto.Projekt.MaterialKilogrammDto(w.name, SUM(aip.kilogramm)) " +
            "FROM ArtikelInProjekt aip " +
            "JOIN aip.artikel a " +
            "JOIN a.werkstoff w " +
            "WHERE aip.projekt.id = :projektId " +
            "GROUP BY w.name",
    )
    fun sumKilogrammByProjektGroupedByWerkstoff(@Param("projektId") projektId: Long?): List<MaterialKilogrammDto>
}
