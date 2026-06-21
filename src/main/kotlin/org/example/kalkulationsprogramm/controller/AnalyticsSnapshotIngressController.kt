package org.example.kalkulationsprogramm.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Validator
import org.example.kalkulationsprogramm.dto.WebsiteAnalytics.AnalyticsSnapshotRequestDto
import org.example.kalkulationsprogramm.service.WebsiteAnalyticsSnapshotService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/internal/analytics-snapshot")
class AnalyticsSnapshotIngressController(
    private val service: WebsiteAnalyticsSnapshotService,
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun empfangeSnapshot(@RequestBody rawPayload: String?): ResponseEntity<Map<String, Any?>> {
        if (rawPayload != null && rawPayload.length > MAX_BODY_BYTES) {
            log.warn("Analytics-Snapshot abgelehnt: Payload zu gross ({} Zeichen).", rawPayload.length)
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(mapOf("success" to false, "message" to "Payload zu gross."))
        }
        val dto = try {
            objectMapper.readValue(rawPayload, AnalyticsSnapshotRequestDto::class.java)
        } catch (e: JsonProcessingException) {
            log.warn("Analytics-Snapshot Payload ist kein gueltiges JSON: {}", e.originalMessage)
            return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "Payload ist kein gueltiges JSON."))
        }

        val violations = validator.validate(dto)
        if (violations.isNotEmpty()) {
            val details = violations.stream().map { "${it.propertyPath} ${it.message}" }.collect(Collectors.joining(", "))
            log.warn("Analytics-Snapshot Validierung fehlgeschlagen: {}", details)
            return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "Validierung fehlgeschlagen.", "details" to details))
        }

        return try {
            val saved = service.upsert(dto, rawPayload)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf("success" to true, "snapshotDate" to saved.snapshotDate.toString(), "id" to saved.id)
            )
        } catch (e: IllegalArgumentException) {
            log.warn("Analytics-Snapshot abgelehnt: {}", e.message)
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to e.message))
        } catch (e: Exception) {
            log.error("Fehler beim Speichern des Analytics-Snapshots", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("success" to false, "message" to "Interner Fehler."))
        }
    }

    companion object {
        const val MAX_BODY_BYTES = 1_000_000
        private val log = LoggerFactory.getLogger(AnalyticsSnapshotIngressController::class.java)
    }
}
