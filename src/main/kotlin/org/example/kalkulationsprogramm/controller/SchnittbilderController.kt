package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.Artikel
import org.example.kalkulationsprogramm.domain.Kategorie
import org.example.kalkulationsprogramm.domain.Schnittbilder
import org.example.kalkulationsprogramm.dto.Schnittbilder.SchnittbildResponseDto
import org.example.kalkulationsprogramm.repository.ArtikelRepository
import org.example.kalkulationsprogramm.repository.KategorieRepository
import org.example.kalkulationsprogramm.repository.SchnittbilderRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/schnittbilder")
class SchnittbilderController(
    private val schnittbilderRepository: SchnittbilderRepository,
    private val artikelRepository: ArtikelRepository,
    private val kategorieRepository: KategorieRepository,
) {
    @GetMapping
    fun list(
        @RequestParam(value = "artikelId", required = false) artikelId: Long?,
        @RequestParam(value = "subKategorieId", required = false) subKategorieId: Int?,
        @RequestParam(value = "kategorieId", required = false) kategorieId: Int?,
    ): ResponseEntity<List<SchnittbildResponseDto>> {
        var effectiveRoot: Int? = null

        if (artikelId != null) {
            val artikel = artikelRepository.findById(artikelId).orElse(null)
            val kategorie = artikel?.let { readKategorie(it, "kategorie") }
            if (artikel == null || kategorie == null) {
                return ResponseEntity.ok(emptyList())
            }
            val rootId = rootKategorieId(kategorie)
            if (!isAllowedRoot(rootId)) {
                return ResponseEntity.ok(emptyList())
            }
            effectiveRoot = rootId
        } else if (subKategorieId != null) {
            val kategorie = kategorieRepository.findById(subKategorieId).orElse(null)
                ?: return ResponseEntity.ok(emptyList())
            val rootId = rootKategorieId(kategorie)
            if (!isAllowedRoot(rootId)) return ResponseEntity.ok(emptyList())
            effectiveRoot = rootId
        } else if (kategorieId != null) {
            effectiveRoot = kategorieId
        }

        if (!isAllowedRoot(effectiveRoot)) {
            return ResponseEntity.ok(emptyList())
        }

        val result = schnittbilderRepository.findByKategorie_Id(effectiveRoot)
            .map { toDto(it) }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<SchnittbildResponseDto> =
        schnittbilderRepository.findById(id)
            .map { toDto(it) }
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    private fun isAllowedRoot(id: Int?): Boolean = id != null && (id == 64 || id == 65)

    private fun rootKategorieId(kategorie: Kategorie): Int? {
        var current = kategorie
        while (readKategorie(current, "parentKategorie") != null) {
            current = readKategorie(current, "parentKategorie") ?: break
        }
        return readIntProperty(current, "id")
    }

    private fun toDto(schnittbild: Schnittbilder): SchnittbildResponseDto {
        val dto = SchnittbildResponseDto()
        dto.id = readLongProperty(schnittbild, "id")
        dto.bildUrlSchnittbild = readStringProperty(schnittbild, "bildUrlSchnittbild")
        dto.form = readStringProperty(schnittbild, "form")
        dto.kategorieId = readKategorie(schnittbild, "kategorie")?.let { readIntProperty(it, "id") }
        return dto
    }

    companion object {
        private fun readKategorie(target: Any, property: String): Kategorie? =
            readProperty(target, property) as? Kategorie

        private fun readStringProperty(target: Any, property: String): String? =
            readProperty(target, property) as? String

        private fun readLongProperty(target: Any, property: String): Long? =
            readProperty(target, property) as? Long

        private fun readIntProperty(target: Any, property: String): Int? =
            readProperty(target, property) as? Int

        private fun readProperty(target: Any, property: String): Any? {
            val suffix = property.replaceFirstChar { it.uppercaseChar() }
            return target.javaClass.methods
                .firstOrNull { it.name == "get$suffix" && it.parameterCount == 0 }
                ?.invoke(target)
        }
    }
}
