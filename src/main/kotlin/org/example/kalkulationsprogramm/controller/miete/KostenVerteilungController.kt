package org.example.kalkulationsprogramm.controller.miete

import org.example.kalkulationsprogramm.dto.miete.KostenpositionDto
import org.example.kalkulationsprogramm.dto.miete.KostenstelleDto
import org.example.kalkulationsprogramm.dto.miete.VerteilungsschluesselDto
import org.example.kalkulationsprogramm.mapper.MieteMapper
import org.example.kalkulationsprogramm.service.miete.KostenVerteilungService
import org.springframework.http.HttpStatus
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

@RestController
@RequestMapping("/api/miete")
class KostenVerteilungController(
    private val kostenVerteilungService: KostenVerteilungService,
    private val mapper: MieteMapper,
) {
    @GetMapping("/mietobjekte/{mietobjektId}/kostenstellen")
    fun listKostenstellen(@PathVariable mietobjektId: Long): List<KostenstelleDto> {
        return kostenVerteilungService.getKostenstellen(mietobjektId).map(mapper::toDto)
    }

    @PostMapping("/mietobjekte/{mietobjektId}/kostenstellen")
    @ResponseStatus(HttpStatus.CREATED)
    fun createKostenstelle(@PathVariable mietobjektId: Long, @RequestBody dto: KostenstelleDto): KostenstelleDto {
        dto.id = null
        return mapper.toDto(kostenVerteilungService.saveKostenstelle(mietobjektId, mapper.toEntity(dto)))
    }

    @PutMapping("/mietobjekte/{mietobjektId}/kostenstellen/{kostenstelleId}")
    fun updateKostenstelle(
        @PathVariable mietobjektId: Long,
        @PathVariable kostenstelleId: Long,
        @RequestBody dto: KostenstelleDto,
    ): KostenstelleDto {
        dto.id = kostenstelleId
        return mapper.toDto(kostenVerteilungService.saveKostenstelle(mietobjektId, mapper.toEntity(dto)))
    }

    @DeleteMapping("/kostenstellen/{kostenstelleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteKostenstelle(@PathVariable kostenstelleId: Long) {
        kostenVerteilungService.deleteKostenstelle(kostenstelleId)
    }

    @GetMapping("/kostenstellen/{kostenstelleId}/kostenpositionen")
    fun listKostenpositionen(
        @PathVariable kostenstelleId: Long,
        @RequestParam(name = "jahr", required = false) jahr: Int?,
    ): List<KostenpositionDto> {
        return kostenVerteilungService.getKostenpositionen(kostenstelleId, jahr).map(mapper::toDto)
    }

    @PostMapping("/kostenstellen/{kostenstelleId}/kostenpositionen")
    @ResponseStatus(HttpStatus.CREATED)
    fun createKostenposition(@PathVariable kostenstelleId: Long, @RequestBody dto: KostenpositionDto): KostenpositionDto {
        dto.id = null
        return mapper.toDto(kostenVerteilungService.saveKostenposition(kostenstelleId, mapper.toEntity(dto)))
    }

    @PutMapping("/kostenstellen/{kostenstelleId}/kostenpositionen/{kostenpositionId}")
    fun updateKostenposition(
        @PathVariable kostenstelleId: Long,
        @PathVariable kostenpositionId: Long,
        @RequestBody dto: KostenpositionDto,
    ): KostenpositionDto {
        dto.id = kostenpositionId
        return mapper.toDto(kostenVerteilungService.saveKostenposition(kostenstelleId, mapper.toEntity(dto)))
    }

    @DeleteMapping("/kostenpositionen/{kostenpositionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteKostenposition(@PathVariable kostenpositionId: Long) {
        kostenVerteilungService.deleteKostenposition(kostenpositionId)
    }

    @PostMapping("/mietobjekte/{mietobjektId}/kostenpositionen/copy-vorjahr")
    fun copyKostenpositionenVonVorjahr(
        @PathVariable mietobjektId: Long,
        @RequestParam("zielJahr") zielJahr: Int,
    ): Map<String, Any> {
        val kopiert = kostenVerteilungService.copyKostenpositionenVonVorjahr(mietobjektId, zielJahr)
        return mapOf("kopiert" to kopiert, "zielJahr" to zielJahr)
    }

    @GetMapping("/mietobjekte/{mietobjektId}/verteilungsschluessel")
    fun listVerteilungsschluessel(@PathVariable mietobjektId: Long): List<VerteilungsschluesselDto> {
        return kostenVerteilungService.getVerteilungsschluessel(mietobjektId).map(mapper::toDto)
    }

    @PostMapping("/mietobjekte/{mietobjektId}/verteilungsschluessel")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVerteilungsschluessel(
        @PathVariable mietobjektId: Long,
        @RequestBody dto: VerteilungsschluesselDto,
    ): VerteilungsschluesselDto {
        return mapper.toDto(kostenVerteilungService.saveVerteilungsschluessel(mietobjektId, mapper.toEntity(dto)))
    }

    @PutMapping("/mietobjekte/{mietobjektId}/verteilungsschluessel/{schluesselId}")
    fun updateVerteilungsschluessel(
        @PathVariable mietobjektId: Long,
        @PathVariable schluesselId: Long,
        @RequestBody dto: VerteilungsschluesselDto,
    ): VerteilungsschluesselDto {
        dto.id = schluesselId
        return mapper.toDto(kostenVerteilungService.saveVerteilungsschluessel(mietobjektId, mapper.toEntity(dto)))
    }

    @DeleteMapping("/verteilungsschluessel/{schluesselId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteVerteilungsschluessel(@PathVariable schluesselId: Long) {
        kostenVerteilungService.deleteVerteilungsschluessel(schluesselId)
    }
}
