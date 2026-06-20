package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Artikel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ArtikelRepository : JpaRepository<Artikel, Long>, JpaSpecificationExecutor<Artikel> {
    @Query("select a from Artikel a join a.artikelpreis n where lower(trim(n.externeArtikelnummer)) = lower(trim(:nummer))")
    fun findByExterneArtikelnummer(@Param("nummer") nummer: String): Optional<Artikel>

    @Query("select a from Artikel a join a.artikelpreis n where lower(trim(n.externeArtikelnummer)) = lower(trim(:nummer)) and n.lieferant.id = :lieferantId")
    fun findByExterneArtikelnummerAndLieferantId(
        @Param("nummer") nummer: String,
        @Param("lieferantId") lieferantId: Long?,
    ): Optional<Artikel>

    @Query("select distinct a.produktlinie from Artikel a left join a.artikelpreis ap where (ap is null or ap.lieferant.id <> :lieferantId) and a.produktlinie is not null")
    fun findDistinctProduktlinieExcludingLieferant(@Param("lieferantId") lieferantId: Long?): List<String>
}
