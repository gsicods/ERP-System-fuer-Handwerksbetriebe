package org.example.kalkulationsprogramm.controller.miete

import org.example.kalkulationsprogramm.dto.miete.MietobjektDto
import org.example.kalkulationsprogramm.dto.miete.MietparteiDto
import org.example.kalkulationsprogramm.mapper.MieteMapper
import org.example.kalkulationsprogramm.service.miete.MietobjektService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/miete")
class MietobjektController(
    private val mietobjektService: MietobjektService,
    private val mapper: MieteMapper
) {
    @GetMapping("/mietobjekte")
    fun listMietobjekte(): List<MietobjektDto> = mietobjektService.findAll().map(mapper::toDto)

    @PostMapping("/mietobjekte")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMietobjekt(@RequestBody dto: MietobjektDto): MietobjektDto =
        mapper.toDto(mietobjektService.save(mapper.toEntity(dto)))

    @PutMapping("/mietobjekte/{id}")
    fun updateMietobjekt(@PathVariable id: Long, @RequestBody dto: MietobjektDto): MietobjektDto {
        dto.id = id
        return mapper.toDto(mietobjektService.save(mapper.toEntity(dto)))
    }

    @DeleteMapping("/mietobjekte/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMietobjekt(@PathVariable id: Long) {
        mietobjektService.delete(id)
    }

    @GetMapping("/mietobjekte/{id}/parteien")
    fun listParteien(@PathVariable id: Long): List<MietparteiDto> =
        mietobjektService.getParteien(id).map(mapper::toDto)

    @PostMapping("/mietobjekte/{id}/parteien")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPartei(@PathVariable id: Long, @RequestBody dto: MietparteiDto): MietparteiDto =
        mapper.toDto(mietobjektService.savePartei(id, mapper.toEntity(dto)))

    @PutMapping("/mietobjekte/{id}/parteien/{parteiId}")
    fun updatePartei(@PathVariable id: Long, @PathVariable parteiId: Long, @RequestBody dto: MietparteiDto): MietparteiDto {
        dto.id = parteiId
        return mapper.toDto(mietobjektService.savePartei(id, mapper.toEntity(dto)))
    }

    @DeleteMapping("/parteien/{parteiId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePartei(@PathVariable parteiId: Long) {
        mietobjektService.deletePartei(parteiId)
    }
}
