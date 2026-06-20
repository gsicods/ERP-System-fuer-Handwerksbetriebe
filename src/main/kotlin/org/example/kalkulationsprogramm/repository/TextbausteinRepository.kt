package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Textbaustein
import org.example.kalkulationsprogramm.domain.TextbausteinTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TextbausteinRepository : JpaRepository<Textbaustein, Long> {
    fun findByTypOrderBySortOrderAscNameAsc(typ: TextbausteinTyp): List<Textbaustein>

    @Query("SELECT DISTINCT t FROM Textbaustein t LEFT JOIN FETCH t.dokumenttypen LEFT JOIN FETCH t.placeholders")
    fun findAllWithDokumenttypen(): List<Textbaustein>
}
