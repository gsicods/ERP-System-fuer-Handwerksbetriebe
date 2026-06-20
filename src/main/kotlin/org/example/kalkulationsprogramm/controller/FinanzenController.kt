package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.finanzen.FinanzenDashboardDto
import org.example.kalkulationsprogramm.dto.finanzen.OffenerPostenDto
import org.example.kalkulationsprogramm.dto.finanzen.ZahlungDto
import org.example.kalkulationsprogramm.dto.finanzen.ZahlungErfassenDto
import org.example.kalkulationsprogramm.service.FinanzenService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/finanzen")
class FinanzenController(
    private val service: FinanzenService
) {
    @GetMapping("/dashboard")
    fun dashboard(
        @RequestParam(value = "jahr", required = false) jahr: Int?,
        @RequestParam(value = "monat", required = false) monat: Int?
    ): FinanzenDashboardDto = service.dashboard(jahr, monat)

    @GetMapping("/offene-posten")
    fun offenePosten(): List<OffenerPostenDto> = service.offenePosten()

    @GetMapping("/ausgangs-dokumente/{id}/zahlungen")
    fun zahlungenAusgang(@PathVariable id: Long): List<ZahlungDto> = service.zahlungenAusgang(id)

    @PostMapping("/ausgangs-dokumente/{id}/zahlungen")
    fun zahlungAusgang(
        @PathVariable id: Long,
        @RequestBody dto: ZahlungErfassenDto?
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(service.erfasseAusgangszahlung(id, dto))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }

    @GetMapping("/belege/{id}/zahlungen")
    fun zahlungenBeleg(@PathVariable id: Long): List<ZahlungDto> = service.zahlungenBeleg(id)

    @PostMapping("/belege/{id}/zahlungen")
    fun zahlungBeleg(
        @PathVariable id: Long,
        @RequestBody dto: ZahlungErfassenDto?
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(service.erfasseBelegzahlung(id, dto))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
}
