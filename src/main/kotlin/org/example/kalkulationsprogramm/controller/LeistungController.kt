package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.Leistung.LeistungCreateDto
import org.example.kalkulationsprogramm.dto.Leistung.LeistungDto
import org.example.kalkulationsprogramm.service.LeistungService
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
@RequestMapping("/api/leistungen")
class LeistungController(private val leistungService: LeistungService) {
    @GetMapping
    fun getAll(): List<LeistungDto> = leistungService.getAllLeistungen()

    @PostMapping
    fun create(@RequestBody dto: LeistungCreateDto): ResponseEntity<LeistungDto> = ResponseEntity.ok(leistungService.createLeistung(dto))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dto: LeistungCreateDto): ResponseEntity<LeistungDto> = ResponseEntity.ok(leistungService.updateLeistung(id, dto))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        leistungService.deleteLeistung(id)
        return ResponseEntity.noContent().build()
    }
}
