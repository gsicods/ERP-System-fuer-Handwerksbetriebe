package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.config.FrontendUserPrincipal
import org.example.kalkulationsprogramm.dto.DokumentLockDto
import org.example.kalkulationsprogramm.service.DokumentLockService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dokument-locks")
class DokumentLockController(
    private val service: DokumentLockService
) {
    @PostMapping("/{dokumentTyp}/{dokumentId}/acquire")
    fun acquire(@PathVariable dokumentTyp: String, @PathVariable dokumentId: Long, authentication: Authentication?): ResponseEntity<DokumentLockDto> {
        val principal = principal(authentication) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val normalized = normalizeTyp(dokumentTyp) ?: return ResponseEntity.badRequest().build()
        val result = service.acquire(normalized, dokumentId, principal.id!!, principal.displayName)
        return if (DokumentLockDto.LOCKED_BY_OTHER == result.status()) ResponseEntity.status(HttpStatus.CONFLICT).body(result) else ResponseEntity.ok(result)
    }

    @PostMapping("/{dokumentTyp}/{dokumentId}/heartbeat")
    fun heartbeat(@PathVariable dokumentTyp: String, @PathVariable dokumentId: Long, authentication: Authentication?): ResponseEntity<DokumentLockDto> {
        val principal = principal(authentication) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val normalized = normalizeTyp(dokumentTyp) ?: return ResponseEntity.badRequest().build()
        val result = service.heartbeat(normalized, dokumentId, principal.id!!, principal.displayName)
        return if (DokumentLockDto.LOCKED_BY_OTHER == result.status()) ResponseEntity.status(HttpStatus.CONFLICT).body(result) else ResponseEntity.ok(result)
    }

    @DeleteMapping("/{dokumentTyp}/{dokumentId}")
    fun release(@PathVariable dokumentTyp: String, @PathVariable dokumentId: Long, authentication: Authentication?): ResponseEntity<Void> {
        val principal = principal(authentication) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val normalized = normalizeTyp(dokumentTyp) ?: return ResponseEntity.badRequest().build()
        service.release(normalized, dokumentId, principal.id!!)
        return ResponseEntity.noContent().build()
    }

    private fun principal(authentication: Authentication?): FrontendUserPrincipal? =
        authentication?.principal as? FrontendUserPrincipal

    private fun normalizeTyp(dokumentTyp: String?): String? {
        val upper = dokumentTyp?.trim()?.uppercase() ?: return null
        return if (ERLAUBTE_TYPEN.contains(upper)) upper else null
    }

    companion object {
        private val ERLAUBTE_TYPEN = setOf(DokumentLockService.TYP_AUSGANG, DokumentLockService.TYP_EINGANG)
    }
}
