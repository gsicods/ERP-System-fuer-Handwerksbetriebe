package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.Artikel.KategorieCreateDto
import org.example.kalkulationsprogramm.dto.Artikel.KategorieResponseDto
import org.example.kalkulationsprogramm.service.KategorieService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/artikel/kategorien")
class ArtikelKategorieController(
    private val kategorieService: KategorieService
) {
    @GetMapping("/haupt")
    fun hauptKategorien(): List<KategorieResponseDto> = kategorieService.findeHauptkategorien()

    @GetMapping("/alle")
    fun alleKategorien(): List<KategorieResponseDto> = kategorieService.alleKategorien()

    @GetMapping("/{parentId}/unterkategorien")
    fun unterKategorien(@PathVariable parentId: Int): List<KategorieResponseDto> =
        kategorieService.findeUnterkategorien(parentId)

    @PostMapping
    fun erstelleKategorie(@RequestBody dto: KategorieCreateDto): ResponseEntity<KategorieResponseDto> =
        try {
            ResponseEntity.status(HttpStatus.CREATED).body(kategorieService.erstelleKategorie(dto))
        } catch (_: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
}
