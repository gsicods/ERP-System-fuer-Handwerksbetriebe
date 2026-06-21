package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.LohnabrechnungDto
import org.example.kalkulationsprogramm.service.LohnabrechnungService
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/lohnabrechnungen")
class LohnabrechnungController(
    private val lohnabrechnungService: LohnabrechnungService,
) {
    @GetMapping("/mitarbeiter/{mitarbeiterId}")
    fun getByMitarbeiter(@PathVariable mitarbeiterId: Long): ResponseEntity<List<LohnabrechnungDto>> {
        return ResponseEntity.ok(lohnabrechnungService.findByMitarbeiterId(mitarbeiterId))
    }

    @GetMapping("/jahr/{jahr}")
    fun getByJahr(@PathVariable jahr: Int): ResponseEntity<List<LohnabrechnungDto>> {
        return ResponseEntity.ok(lohnabrechnungService.findByJahr(jahr))
    }

    @GetMapping("/steuerberater/{steuerberaterId}/jahr/{jahr}")
    fun getBySteuerberaterAndJahr(
        @PathVariable steuerberaterId: Long,
        @PathVariable jahr: Int,
    ): ResponseEntity<List<LohnabrechnungDto>> {
        return ResponseEntity.ok(lohnabrechnungService.findBySteuerberaterAndJahr(steuerberaterId, jahr))
    }

    @GetMapping("/jahre")
    fun getAvailableYears(): ResponseEntity<List<Int>> {
        return ResponseEntity.ok(lohnabrechnungService.findAvailableYears())
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<LohnabrechnungDto> {
        val dto = lohnabrechnungService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/{id}/download")
    fun downloadPdf(@PathVariable id: Long): ResponseEntity<Resource> {
        return try {
            lohnabrechnungService.findPdf(id)
                .map { pdf ->
                    try {
                        val resource: Resource = UrlResource(pdf.pfad().toUri())
                        ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${pdf.anzeigeName()}\"")
                            .body(resource)
                    } catch (e: Exception) {
                        ResponseEntity.internalServerError().build<Resource>()
                    }
                }
                .orElse(ResponseEntity.notFound().build())
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        lohnabrechnungService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
