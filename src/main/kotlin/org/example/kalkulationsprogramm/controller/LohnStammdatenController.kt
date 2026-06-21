package org.example.kalkulationsprogramm.controller

import jakarta.validation.Valid
import org.example.kalkulationsprogramm.dto.GewerkDto
import org.example.kalkulationsprogramm.dto.KrankenkasseDto
import org.example.kalkulationsprogramm.dto.SvSatzDto
import org.example.kalkulationsprogramm.service.GewerkService
import org.example.kalkulationsprogramm.service.KrankenkasseService
import org.example.kalkulationsprogramm.service.SvSatzService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/lohn-stammdaten")
class LohnStammdatenController(
    private val krankenkasseService: KrankenkasseService,
    private val svSatzService: SvSatzService,
    private val gewerkService: GewerkService,
) {
    @GetMapping("/krankenkassen")
    fun getKrankenkassen(
        @RequestParam(name = "nurAktive", required = false, defaultValue = "false") nurAktive: Boolean,
    ): ResponseEntity<List<KrankenkasseDto>> {
        return ResponseEntity.ok(if (nurAktive) krankenkasseService.findAktiv() else krankenkasseService.findAll())
    }

    @PostMapping("/krankenkassen")
    fun erstelleKrankenkasse(@Valid @RequestBody dto: KrankenkasseDto): ResponseEntity<Any> {
        return try {
            dto.id = null
            ResponseEntity.ok(krankenkasseService.save(dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }
    }

    @PutMapping("/krankenkassen/{id}")
    fun aktualisiereKrankenkasse(@PathVariable id: Long, @Valid @RequestBody dto: KrankenkasseDto): ResponseEntity<Any> {
        return try {
            dto.id = id
            ResponseEntity.ok(krankenkasseService.save(dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }
    }

    @DeleteMapping("/krankenkassen/{id}")
    fun loescheKrankenkasse(@PathVariable id: Long): ResponseEntity<Void> {
        krankenkasseService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/sv-saetze")
    fun getSvSaetze(): ResponseEntity<List<SvSatzDto>> = ResponseEntity.ok(svSatzService.findAll())

    @PostMapping("/sv-saetze")
    fun erstelleSvSatz(@Valid @RequestBody dto: SvSatzDto): ResponseEntity<Any> {
        return try {
            dto.id = null
            ResponseEntity.ok(svSatzService.save(dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }
    }

    @PutMapping("/sv-saetze/{id}")
    fun aktualisiereSvSatz(@PathVariable id: Long, @Valid @RequestBody dto: SvSatzDto): ResponseEntity<Any> {
        return try {
            dto.id = id
            ResponseEntity.ok(svSatzService.save(dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }
    }

    @DeleteMapping("/sv-saetze/{id}")
    fun loescheSvSatz(@PathVariable id: Long): ResponseEntity<Void> {
        svSatzService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/gewerke")
    fun getGewerke(
        @RequestParam(name = "nurAktive", required = false, defaultValue = "false") nurAktive: Boolean,
    ): ResponseEntity<List<GewerkDto>> {
        return ResponseEntity.ok(if (nurAktive) gewerkService.findAktiv() else gewerkService.findAll())
    }

    @PostMapping("/gewerke")
    fun erstelleGewerk(@Valid @RequestBody dto: GewerkDto): ResponseEntity<Any> {
        return try {
            dto.id = null
            ResponseEntity.ok(gewerkService.save(dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }
    }

    @PutMapping("/gewerke/{id}")
    fun aktualisiereGewerk(@PathVariable id: Long, @Valid @RequestBody dto: GewerkDto): ResponseEntity<Any> {
        return try {
            dto.id = id
            ResponseEntity.ok(gewerkService.save(dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }
    }

    @DeleteMapping("/gewerke/{id}")
    fun loescheGewerk(@PathVariable id: Long): ResponseEntity<Void> {
        gewerkService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
