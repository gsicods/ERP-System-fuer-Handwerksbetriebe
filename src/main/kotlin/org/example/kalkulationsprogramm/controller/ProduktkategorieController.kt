package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.Produktkategroie.ProduktkategorieAnalyseDto
import org.example.kalkulationsprogramm.dto.Produktkategroie.ProduktkategorieErstellenDto
import org.example.kalkulationsprogramm.dto.Produktkategroie.ProduktkategorieResponseDto
import org.example.kalkulationsprogramm.service.ProduktkategorieService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/produktkategorien")
class ProduktkategorieController(
    private val produktkategorieService: ProduktkategorieService
) {
    @GetMapping
    fun getAlleKategorien(@RequestParam(value = "light", required = false, defaultValue = "false") light: Boolean): ResponseEntity<List<ProduktkategorieResponseDto>> =
        ResponseEntity.ok(produktkategorieService.findeAlleKategorien(light))

    @GetMapping("/haupt")
    fun getHauptkategorien(@RequestParam(value = "light", required = false, defaultValue = "false") light: Boolean): ResponseEntity<List<ProduktkategorieResponseDto>> =
        ResponseEntity.ok(produktkategorieService.findeHauptkategorien(light))

    @GetMapping("/{id}")
    fun getKategorieById(@PathVariable id: Long): ResponseEntity<ProduktkategorieResponseDto> =
        try {
            ResponseEntity.ok(produktkategorieService.findeKategorieById(id))
        } catch (_: RuntimeException) {
            ResponseEntity.notFound().build()
        }

    @GetMapping("/{kategorieId}/analyse")
    fun getAnalyse(@PathVariable kategorieId: Long, @RequestParam(value = "jahr", required = false) jahr: Int?): ResponseEntity<ProduktkategorieAnalyseDto> =
        ResponseEntity.ok(produktkategorieService.analysiereKategorie(kategorieId, jahr))

    @GetMapping("/{parentId}/unterkategorien")
    fun getUnterkategorien(
        @PathVariable parentId: Long,
        @RequestParam(value = "light", required = false, defaultValue = "false") light: Boolean
    ): ResponseEntity<List<ProduktkategorieResponseDto>> =
        ResponseEntity.ok(produktkategorieService.findeUnterkategorie(parentId, light))

    @GetMapping("/suche")
    fun sucheLeafKategorien(@RequestParam("q") suchbegriff: String): ResponseEntity<List<ProduktkategorieResponseDto>> =
        ResponseEntity.ok(produktkategorieService.sucheLeafKategorien(suchbegriff))

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun erstelleKategorie(
        @ModelAttribute kategorieDto: ProduktkategorieErstellenDto,
        @RequestParam(value = "bild", required = false) bild: MultipartFile?
    ): ResponseEntity<ProduktkategorieResponseDto> =
        ResponseEntity(produktkategorieService.erstelleKategorie(kategorieDto, bild), HttpStatus.CREATED)

    @PatchMapping("/{kategorieId}/beschreibung")
    fun aktualisiereBeschreibung(
        @PathVariable kategorieId: Long,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<ProduktkategorieResponseDto> =
        try {
            ResponseEntity.ok(produktkategorieService.aktualisiereBeschreibung(kategorieId, body.getOrDefault("beschreibung", "")))
        } catch (_: RuntimeException) {
            ResponseEntity.notFound().build()
        }

    @PutMapping(value = ["/{id}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun aktualisiereKategorie(
        @PathVariable id: Long,
        @ModelAttribute kategorieDto: ProduktkategorieErstellenDto,
        @RequestParam(value = "bild", required = false) bild: MultipartFile?
    ): ResponseEntity<ProduktkategorieResponseDto> =
        try {
            ResponseEntity.ok(produktkategorieService.aktualisiereKategorie(id, kategorieDto, bild))
        } catch (_: RuntimeException) {
            ResponseEntity.notFound().build()
        }

    @DeleteMapping("/{kategorieId}")
    fun loescheKategorie(@PathVariable kategorieId: Long): ResponseEntity<Void> =
        try {
            produktkategorieService.loescheKategorie(kategorieId)
            ResponseEntity.noContent().build()
        } catch (_: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        } catch (_: RuntimeException) {
            ResponseEntity.notFound().build()
        }
}
