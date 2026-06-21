package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.config.FrontendUserPrincipal
import org.example.kalkulationsprogramm.service.EntityLastAccessedService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/last-accessed")
class EntityLastAccessedController(
    private val service: EntityLastAccessedService
) {
    @GetMapping("/{entityType}")
    fun list(@PathVariable entityType: String, authentication: Authentication?): ResponseEntity<Map<Long, Long>> {
        val principal = principal(authentication) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val normalized = normalize(entityType) ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(service.listForUser(principal.id, normalized))
    }

    @PostMapping("/{entityType}/{entityId}")
    fun track(
        @PathVariable entityType: String,
        @PathVariable entityId: Long?,
        authentication: Authentication?
    ): ResponseEntity<Void> {
        val principal = principal(authentication) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val normalized = normalize(entityType)
        if (normalized == null || entityId == null) {
            return ResponseEntity.badRequest().build()
        }
        service.track(principal.id, normalized, entityId)
        return ResponseEntity.noContent().build()
    }

    private fun principal(authentication: Authentication?): FrontendUserPrincipal? =
        authentication?.principal as? FrontendUserPrincipal

    private fun normalize(entityType: String?): String? {
        val upper = entityType?.trim()?.uppercase() ?: return null
        return if (ALLOWED_TYPES.contains(upper)) upper else null
    }

    companion object {
        private val ALLOWED_TYPES = setOf("PROJEKT", "ANFRAGE")
    }
}
