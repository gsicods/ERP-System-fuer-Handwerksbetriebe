package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.Bestellung.BestellungResponseDto
import org.example.kalkulationsprogramm.service.BestellungPdfService
import org.example.kalkulationsprogramm.service.BestellungService
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files

@RestController
@RequestMapping("/api/bestellungen")
class BestellungController(
    private val bestellungService: BestellungService,
    private val bestellungPdfService: BestellungPdfService
) {
    @GetMapping("/offen")
    fun offeneBestellungen(): List<BestellungResponseDto> = bestellungService.findeOffeneBestellungen()

    @PatchMapping("/{id}")
    fun setBestellt(@PathVariable id: Long, @RequestParam bestellt: Boolean): ResponseEntity<Void> {
        bestellungService.setBestellt(id, bestellt)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/projekt/{projektId}/pdf")
    fun pdfForProjekt(@PathVariable projektId: Long): ResponseEntity<Resource> {
        val pdf = bestellungPdfService.generatePdfForProjekt(projektId)
        val res: Resource = InputStreamResource(Files.newInputStream(pdf))
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=bestellung.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(res)
    }

    @GetMapping("/lieferant/{lieferantId}/pdf")
    fun pdfForLieferant(@PathVariable lieferantId: Long): ResponseEntity<Resource> {
        val pdf = bestellungPdfService.generatePdfForLieferant(lieferantId)
        val res: Resource = InputStreamResource(Files.newInputStream(pdf))
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=bestellung-lieferant.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(res)
    }
}
