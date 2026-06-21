package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.Abteilung
import org.example.kalkulationsprogramm.dto.Abteilung.AbteilungResponseDto
import org.example.kalkulationsprogramm.dto.Arbeitsgang.ArbeitsgangErstellenDto
import org.example.kalkulationsprogramm.dto.Arbeitsgang.ArbeitsgangResponseDto
import org.example.kalkulationsprogramm.dto.Arbeitsgang.ArbeitsgangStundensatzDto
import org.example.kalkulationsprogramm.mapper.ArbeitsgangMapper
import org.example.kalkulationsprogramm.repository.AbteilungRepository
import org.example.kalkulationsprogramm.service.ArbeitsgangManagementService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api")
class ArbeitsgangController(
    private val arbeitsgangManagementService: ArbeitsgangManagementService,
    private val arbeitsgangMapper: ArbeitsgangMapper,
    private val abteilungRepository: AbteilungRepository,
) {
    @GetMapping("/abteilungen")
    fun getAlleAbteilungen(): ResponseEntity<List<AbteilungResponseDto>> {
        return ResponseEntity.ok(abteilungRepository.findAll().map(::toAbteilungDto))
    }

    @PostMapping("/abteilungen")
    fun erstelleAbteilung(@RequestBody body: Map<String, String>): ResponseEntity<AbteilungResponseDto> {
        val name = body["name"]
        if (name.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val abteilung = Abteilung().apply { this.name = name.trim() }
        return ResponseEntity.status(HttpStatus.CREATED).body(toAbteilungDto(abteilungRepository.save(abteilung)))
    }

    @DeleteMapping("/abteilungen/{id}")
    fun loescheAbteilung(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            val abteilung = abteilungRepository.findById(id)
                .orElseThrow { RuntimeException("Abteilung nicht gefunden") }
            if (abteilung.arbeitsgaenge.isNotEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
            abteilungRepository.delete(abteilung)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/arbeitsgaenge")
    fun erstelleNeuenArbeitsgang(@RequestBody arbeitsgangDto: ArbeitsgangErstellenDto): ResponseEntity<ArbeitsgangResponseDto> {
        val gespeicherterArbeitsgang = arbeitsgangManagementService.erstelleArbeitsgang(arbeitsgangDto)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(arbeitsgangMapper.toArbeitsgangResponseDto(gespeicherterArbeitsgang))
    }

    @DeleteMapping("/arbeitsgaenge/{arbeitsgangID}")
    fun loescheArbeitsgang(@PathVariable arbeitsgangID: Long): ResponseEntity<Void> {
        return try {
            arbeitsgangManagementService.loescheArbeitsgang(arbeitsgangID)
            ResponseEntity.noContent().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/arbeitsgaenge")
    fun getAlleArbeitsgaenge(): ResponseEntity<List<ArbeitsgangResponseDto>> {
        val responseDtos = arbeitsgangManagementService.findeAlle()
            .map(arbeitsgangMapper::toArbeitsgangResponseDto)
            .filterNotNull()
        return ResponseEntity.ok(responseDtos)
    }

    @PostMapping("/arbeitsgaenge/stundensaetze")
    fun aktualisiereStundensaetze(@RequestBody dtos: List<ArbeitsgangStundensatzDto>): ResponseEntity<Void> {
        arbeitsgangManagementService.aktualisiereStundensaetze(dtos)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/arbeitsgaenge/{id}/stundensatz")
    fun aktualisiereEinzelnenStundensatz(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Any>,
    ): ResponseEntity<ArbeitsgangResponseDto> {
        return try {
            val neuerSatz = BigDecimal(body["stundensatz"].toString())
            arbeitsgangManagementService.aktualisiereEinzelnenStundensatz(id, neuerSatz)
            val arbeitsgang = arbeitsgangManagementService.findeAlle()
                .firstOrNull { it.id == id }
                ?: throw RuntimeException("Arbeitsgang nicht gefunden")
            ResponseEntity.ok(arbeitsgangMapper.toArbeitsgangResponseDto(arbeitsgang))
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    private fun toAbteilungDto(abteilung: Abteilung): AbteilungResponseDto {
        return AbteilungResponseDto().apply {
            id = abteilung.id
            name = abteilung.name
        }
    }
}
