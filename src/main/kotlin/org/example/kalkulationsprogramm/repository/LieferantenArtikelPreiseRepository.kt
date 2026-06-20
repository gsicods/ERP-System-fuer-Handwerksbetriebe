package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.LieferantenArtikelPreise
import org.example.kalkulationsprogramm.domain.LieferantenArtikelPreiseId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface LieferantenArtikelPreiseRepository :
    JpaRepository<LieferantenArtikelPreise, LieferantenArtikelPreiseId>,
    JpaSpecificationExecutor<LieferantenArtikelPreise> {
    fun findByArtikel_IdAndLieferant_Id(artikelId: Long?, lieferantId: Long?): Optional<LieferantenArtikelPreise>

    fun findByExterneArtikelnummerIgnoreCaseAndLieferant_Id(
        externeArtikelnummer: String,
        lieferantId: Long?,
    ): Optional<LieferantenArtikelPreise>
}
