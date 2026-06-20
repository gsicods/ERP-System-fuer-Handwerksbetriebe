package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Artikel
import org.example.kalkulationsprogramm.domain.Lieferanten
import org.example.kalkulationsprogramm.dto.Artikel.ArtikelCreateDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification

interface ArtikelServiceContract {
    fun erstelleArtikel(dto: ArtikelCreateDto): Artikel

    fun findeAlleByIds(ids: List<Long>): List<Artikel>

    fun suche(specification: Specification<Artikel>, pageable: Pageable): Page<Artikel>

    fun findeProduktlinienOhneLieferant(lieferantId: Long?): List<String>

    fun fuegeExterneNummerHinzu(artikelId: Long?, lieferant: Lieferanten, nummer: String)
}
