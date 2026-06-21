package org.example.kalkulationsprogramm.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import org.example.kalkulationsprogramm.dto.Anfrage.AnfrageFunnelRequestDto
import org.example.kalkulationsprogramm.service.AnfrageFunnelService
import org.example.kalkulationsprogramm.service.FunnelAnfrageAbgelehntException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/api/internal/anfrage")
class AnfrageFunnelController(
    private val anfrageFunnelService: AnfrageFunnelService,
    private val objectMapper: ObjectMapper,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun empfangeFunnelAnfrage(
        @RequestPart("anfrage") anfrageJson: String,
        @RequestPart(value = "bilder", required = false) bilder: List<MultipartFile>?,
    ): ResponseEntity<Map<String, Any?>> {
        val dto = try {
            objectMapper.readValue(anfrageJson, AnfrageFunnelRequestDto::class.java)
        } catch (e: IOException) {
            return ResponseEntity.badRequest()
                .body(mapOf("success" to false, "message" to "Anfrage-Payload ist kein gültiges JSON."))
        }
        return verarbeite(dto, bilder)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun empfangeFunnelAnfrageJson(@Valid @RequestBody dto: AnfrageFunnelRequestDto): ResponseEntity<Map<String, Any?>> {
        return verarbeite(dto, emptyList())
    }

    private fun verarbeite(dto: AnfrageFunnelRequestDto, bilder: List<MultipartFile>?): ResponseEntity<Map<String, Any?>> {
        return try {
            val anfrage = anfrageFunnelService.verarbeiteFunnelAnfrage(dto, bilder)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "success" to true,
                    "anfrageId" to anfrage.id,
                    "message" to "Anfrage erfolgreich angelegt.",
                ),
            )
        } catch (e: FunnelAnfrageAbgelehntException) {
            log.info("Funnel-Anfrage als Spam abgelehnt: {}", e.message)
            ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(
                    mapOf(
                        "success" to false,
                        "code" to "SPAM_ABGELEHNT",
                        "message" to (e.message ?: "Anfrage wirkt nicht ernst gemeint und konnte nicht gesendet werden."),
                    ),
                )
        } catch (e: IllegalArgumentException) {
            log.warn("Funnel-Anfrage abgelehnt: {}", e.message)
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to e.message.orEmpty()))
        } catch (e: IllegalStateException) {
            log.warn("Funnel-Anfrage abgelehnt: {}", e.message)
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to e.message.orEmpty()))
        } catch (e: Exception) {
            log.error("Fehler bei Funnel-Anfrage", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("success" to false, "message" to "Interner Fehler."))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AnfrageFunnelController::class.java)
    }
}
