package org.example.kalkulationsprogramm.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.example.kalkulationsprogramm.dto.Verrechnungslohn.VerrechnungslohnErgebnisDto
import org.example.kalkulationsprogramm.dto.Verrechnungslohn.VerrechnungslohnUebernehmenRequest
import org.example.kalkulationsprogramm.service.VerrechnungslohnService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/verrechnungslohn")
@Validated
class VerrechnungslohnController(
    private val verrechnungslohnService: VerrechnungslohnService
) {
    @GetMapping
    fun berechne(@RequestParam @Min(2000) @Max(2100) jahr: Int): ResponseEntity<VerrechnungslohnErgebnisDto> =
        ResponseEntity.ok(verrechnungslohnService.berechne(jahr))

    @PostMapping("/uebernehmen")
    fun uebernehmen(
        @Valid @RequestBody request: VerrechnungslohnUebernehmenRequest
    ): ResponseEntity<Map<String, Any>> {
        val aktualisiert = verrechnungslohnService.uebernehmen(request)
        return ResponseEntity.ok(
            mapOf("aktualisierteArbeitsgaenge" to aktualisiert, "jahr" to request.jahr)
        )
    }
}
