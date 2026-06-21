package org.example.kalkulationsprogramm.controller.miete

import org.example.kalkulationsprogramm.dto.miete.AnnualAccountingResponseDto
import org.example.kalkulationsprogramm.mapper.MieteMapper
import org.example.kalkulationsprogramm.service.miete.MietabrechnungPdfService
import org.example.kalkulationsprogramm.service.miete.MietabrechnungService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/miete/mietobjekte/{mietobjektId}/jahresabrechnung")
class MietabrechnungController(
    private val mietabrechnungService: MietabrechnungService,
    private val mietabrechnungPdfService: MietabrechnungPdfService,
    private val mapper: MieteMapper
) {

    @GetMapping
    fun getJahresabrechnung(
        @PathVariable mietobjektId: Long,
        @RequestParam("jahr") jahr: Int
    ): AnnualAccountingResponseDto {
        val result = mietabrechnungService.berechneJahresabrechnung(mietobjektId, jahr)
        return mapper.toDto(result)
    }

    @GetMapping(value = ["/pdf"], produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadJahresabrechnung(
        @PathVariable mietobjektId: Long,
        @RequestParam("jahr") jahr: Int
    ): ResponseEntity<ByteArray> {
        val pdf = mietabrechnungPdfService.generatePdf(mietobjektId, jahr)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=jahresabrechnung-$mietobjektId-$jahr.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf)
    }
}
