package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.Zahlungsart
import org.example.kalkulationsprogramm.repository.ZahlungsartRepository
import org.example.kalkulationsprogramm.service.BelegService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/buchhaltung")
class ZahlungsartController(
    private val zahlungsartRepository: ZahlungsartRepository,
    private val belegService: BelegService
) {
    @GetMapping("/zahlungsarten")
    fun list(
        @RequestParam(value = "nurAktive", defaultValue = "true") nurAktive: Boolean,
        @RequestParam(value = "token", required = false) token: String?,
        auth: Authentication?
    ): ResponseEntity<List<Map<String, Any?>>> {
        val caller = belegService.findCaller(token, auth)
        if (caller == null || !belegService.darfSehen(caller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val zahlungsarten = if (nurAktive) {
            zahlungsartRepository.findByAktivTrueOrderBySortierungAscBezeichnungAsc()
        } else {
            zahlungsartRepository.findAllByOrderBySortierungAscBezeichnungAsc()
        }
        return ResponseEntity.ok(zahlungsarten.map(::toDto))
    }

    companion object {
        private fun toDto(zahlungsart: Zahlungsart): Map<String, Any?> = mapOf(
            "id" to zahlungsart.id,
            "bezeichnung" to zahlungsart.bezeichnung,
            "aktiv" to zahlungsart.isAktiv(),
            "sortierung" to zahlungsart.sortierung
        )
    }
}
