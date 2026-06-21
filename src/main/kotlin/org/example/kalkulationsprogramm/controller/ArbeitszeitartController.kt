package org.example.kalkulationsprogramm.controller

import jakarta.validation.Valid
import org.example.kalkulationsprogramm.dto.Arbeitszeitart.ArbeitszeitartCreateDto
import org.example.kalkulationsprogramm.dto.Arbeitszeitart.ArbeitszeitartDto
import org.example.kalkulationsprogramm.service.ArbeitszeitartService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/arbeitszeitarten")
class ArbeitszeitartController(
    private val service: ArbeitszeitartService
) {
    @GetMapping
    fun getAll(): List<ArbeitszeitartDto> = service.findAllAktiv()

    @GetMapping("/alle")
    fun getAllInclInactive(): List<ArbeitszeitartDto> = service.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ArbeitszeitartDto = service.findById(id)

    @PostMapping
    fun create(@Valid @RequestBody dto: ArbeitszeitartCreateDto): ArbeitszeitartDto = service.create(dto)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody dto: ArbeitszeitartCreateDto): ArbeitszeitartDto =
        service.update(id, dto)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/deaktivieren")
    fun deaktivieren(@PathVariable id: Long): ResponseEntity<Void> {
        service.deaktivieren(id)
        return ResponseEntity.noContent().build()
    }
}
