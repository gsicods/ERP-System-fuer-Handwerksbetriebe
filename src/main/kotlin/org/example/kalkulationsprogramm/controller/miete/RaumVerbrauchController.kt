package org.example.kalkulationsprogramm.controller.miete

import org.example.kalkulationsprogramm.dto.miete.RaumDto
import org.example.kalkulationsprogramm.dto.miete.VerbrauchsgegenstandDto
import org.example.kalkulationsprogramm.dto.miete.ZaehlerstandDto
import org.example.kalkulationsprogramm.mapper.MieteMapper
import org.example.kalkulationsprogramm.service.miete.RaumVerbrauchService
import org.example.kalkulationsprogramm.service.miete.ZaehlerstandErfassungsPdfService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/miete")
class RaumVerbrauchController(
    private val raumVerbrauchService: RaumVerbrauchService,
    private val mapper: MieteMapper,
    private val zaehlerstandErfassungsPdfService: ZaehlerstandErfassungsPdfService,
) {
    @GetMapping("/mietobjekte/{mietobjektId}/raeume")
    fun listRaeume(@PathVariable mietobjektId: Long): List<RaumDto> {
        return raumVerbrauchService.getRaeume(mietobjektId).map(mapper::toDto)
    }

    @PostMapping("/mietobjekte/{mietobjektId}/raeume")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRaum(@PathVariable mietobjektId: Long, @RequestBody dto: RaumDto): RaumDto {
        return mapper.toDto(raumVerbrauchService.saveRaum(mietobjektId, mapper.toEntity(dto)))
    }

    @PutMapping("/mietobjekte/{mietobjektId}/raeume/{raumId}")
    fun updateRaum(@PathVariable mietobjektId: Long, @PathVariable raumId: Long, @RequestBody dto: RaumDto): RaumDto {
        dto.id = raumId
        return mapper.toDto(raumVerbrauchService.saveRaum(mietobjektId, mapper.toEntity(dto)))
    }

    @DeleteMapping("/raeume/{raumId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRaum(@PathVariable raumId: Long) {
        raumVerbrauchService.deleteRaum(raumId)
    }

    @GetMapping("/raeume/{raumId}/verbrauchsgegenstaende")
    fun listVerbrauchsgegenstaende(@PathVariable raumId: Long): List<VerbrauchsgegenstandDto> {
        return raumVerbrauchService.getVerbrauchsgegenstaende(raumId).map(mapper::toDto)
    }

    @PostMapping("/raeume/{raumId}/verbrauchsgegenstaende")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVerbrauchsgegenstand(@PathVariable raumId: Long, @RequestBody dto: VerbrauchsgegenstandDto): VerbrauchsgegenstandDto {
        return mapper.toDto(raumVerbrauchService.saveVerbrauchsgegenstand(raumId, mapper.toEntity(dto)))
    }

    @PutMapping("/raeume/{raumId}/verbrauchsgegenstaende/{gegenstandId}")
    fun updateVerbrauchsgegenstand(
        @PathVariable raumId: Long,
        @PathVariable gegenstandId: Long,
        @RequestBody dto: VerbrauchsgegenstandDto,
    ): VerbrauchsgegenstandDto {
        dto.id = gegenstandId
        return mapper.toDto(raumVerbrauchService.saveVerbrauchsgegenstand(raumId, mapper.toEntity(dto)))
    }

    @DeleteMapping("/verbrauchsgegenstaende/{gegenstandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteVerbrauchsgegenstand(@PathVariable gegenstandId: Long) {
        raumVerbrauchService.deleteVerbrauchsgegenstand(gegenstandId)
    }

    @GetMapping(
        value = ["/mietobjekte/{mietobjektId}/zaehlerstaende/erfassung.pdf"],
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    fun downloadZaehlerstandErfassungsbogen(
        @PathVariable mietobjektId: Long,
        @RequestParam(value = "jahr", required = false) jahr: Int?,
    ): ResponseEntity<ByteArray> {
        val pdf = zaehlerstandErfassungsPdfService.generatePdf(mietobjektId, jahr)
        val yearForName = jahr ?: LocalDate.now().year
        val filename = "zaehlerstaende-ablesebogen-$mietobjektId-$yearForName.pdf"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf)
    }

    @GetMapping("/verbrauchsgegenstaende/{gegenstandId}/zaehlerstaende")
    fun listZaehlerstaende(@PathVariable gegenstandId: Long): List<ZaehlerstandDto> {
        return raumVerbrauchService.getZaehlerstaende(gegenstandId).map(mapper::toDto)
    }

    @PostMapping("/verbrauchsgegenstaende/{gegenstandId}/zaehlerstaende")
    @ResponseStatus(HttpStatus.CREATED)
    fun createZaehlerstand(@PathVariable gegenstandId: Long, @RequestBody dto: ZaehlerstandDto): ZaehlerstandDto {
        return mapper.toDto(raumVerbrauchService.saveZaehlerstand(gegenstandId, mapper.toEntity(dto)))
    }

    @PutMapping("/verbrauchsgegenstaende/{gegenstandId}/zaehlerstaende/{zaehlerstandId}")
    fun updateZaehlerstand(
        @PathVariable gegenstandId: Long,
        @PathVariable zaehlerstandId: Long,
        @RequestBody dto: ZaehlerstandDto,
    ): ZaehlerstandDto {
        dto.id = zaehlerstandId
        return mapper.toDto(raumVerbrauchService.saveZaehlerstand(gegenstandId, mapper.toEntity(dto)))
    }

    @DeleteMapping("/zaehlerstaende/{zaehlerstandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteZaehlerstand(@PathVariable zaehlerstandId: Long) {
        raumVerbrauchService.deleteZaehlerstand(zaehlerstandId)
    }
}
