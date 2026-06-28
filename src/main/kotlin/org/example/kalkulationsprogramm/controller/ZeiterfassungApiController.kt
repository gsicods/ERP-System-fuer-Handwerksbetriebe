package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.Arbeitsgang.ArbeitsgangResponseDto
import org.example.kalkulationsprogramm.service.ZeiterfassungApiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

@RestController
@RequestMapping("/api/zeiterfassung")
class ZeiterfassungApiController(
    private val service: ZeiterfassungApiService,
) {
    @GetMapping("/projekte")
    fun getOpenProjekte(
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<List<Map<String, Any>>> = ResponseEntity.ok(service.getOpenProjekte(limit, search))

    @GetMapping("/kategorien")
    fun getKategorienMitPfad(): ResponseEntity<List<Map<String, Any>>> =
        ResponseEntity.ok(service.getKategorienMitPfad())

    @GetMapping("/kategorien/{projektId}")
    fun getKategorienByProjekt(@PathVariable projektId: Long): ResponseEntity<List<Map<String, Any>>> =
        ResponseEntity.ok(service.getKategorienByProjektId(projektId))

    @GetMapping("/arbeitsgaenge/{token}")
    fun getArbeitsgaengeForMitarbeiter(@PathVariable token: String): ResponseEntity<List<ArbeitsgangResponseDto>> =
        service.getArbeitsgaengeByMitarbeiterToken(token)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @GetMapping("/lieferanten")
    fun getLieferanten(
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<List<Map<String, Any>>> = ResponseEntity.ok(service.getLieferanten(limit, search))

    @PostMapping("/start")
    fun startZeiterfassung(@RequestBody body: Map<String, Any>): ResponseEntity<*> =
        try {
            val token = body["token"] as String
            val projektId = (body["projektId"] as Number).toLong()
            val arbeitsgangId = (body["arbeitsgangId"] as Number).toLong()
            val produktkategorieId = (body["produktkategorieId"] as? Number)?.toLong()
            val originalZeit = parseOptionalDateTime(body["originalZeit"])
            val idempotencyKey = body["idempotencyKey"]?.toString()
            ResponseEntity.ok(
                service.startZeiterfassung(
                    token,
                    projektId,
                    arbeitsgangId,
                    produktkategorieId,
                    originalZeit,
                    idempotencyKey,
                )
            )
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }

    @PostMapping("/stop")
    fun stopZeiterfassung(@RequestBody body: Map<String, Any>): ResponseEntity<*> =
        try {
            val token = body["token"] as String
            val originalZeit = parseOptionalDateTime(body["originalZeit"])
            val idempotencyKey = body["idempotencyKey"]?.toString()
            ResponseEntity.ok(service.stopZeiterfassung(token, originalZeit, idempotencyKey))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }

    @PostMapping("/pause")
    fun startPause(@RequestBody body: Map<String, Any>): ResponseEntity<*> =
        try {
            val token = body["token"] as String
            val originalZeit = parseOptionalDateTime(body["originalZeit"])
            val idempotencyKey = body["idempotencyKey"]?.toString()
            ResponseEntity.ok(service.startPause(token, originalZeit, idempotencyKey))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }

    private fun parseOptionalDateTime(value: Any?): LocalDateTime? {
        if (value == null) {
            return null
        }
        return try {
            val str = value.toString().trim()
            when {
                str.endsWith("Z") -> Instant.parse(str)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()

                str.contains("+") && str.lastIndexOf('+') > 10 -> OffsetDateTime.parse(str)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime()

                else -> LocalDateTime.parse(str)
            }
        } catch (_: Exception) {
            null
        }
    }

    @GetMapping("/aktiv/{token}")
    fun getAktiveBuchung(@PathVariable token: String): ResponseEntity<*> =
        service.getAktiveBuchung(token)
            .map { ResponseEntity.ok(it as Any) }
            .orElse(ResponseEntity.ok(mapOf("aktiv" to false)))

    @GetMapping("/heute/{token}")
    fun getHeuteGearbeitet(@PathVariable token: String): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(service.getHeuteGearbeitet(token))

    @GetMapping("/projekte/{projektId}/bilder")
    fun getProjektBilder(@PathVariable projektId: Long): ResponseEntity<List<Map<String, Any>>> =
        ResponseEntity.ok(service.getProjektBilder(projektId))

    @GetMapping("/buchungszeitfenster/{token}")
    fun getBuchungszeitfenster(@PathVariable token: String): ResponseEntity<*> =
        try {
            ResponseEntity.ok(service.getBuchungszeitfenster(token))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }

    @GetMapping("/saldo/{token}")
    fun getSaldo(
        @PathVariable token: String,
        @RequestParam(required = false) jahr: Int?,
        @RequestParam(required = false) monat: Int?,
        @RequestParam(required = false) gesamtBisHeute: Boolean?,
    ): ResponseEntity<Map<String, Any>> = ResponseEntity.ok(service.getSaldo(token, jahr, monat, gesamtBisHeute))

    @GetMapping("/buchungen/{token}")
    fun getBuchungen(
        @PathVariable token: String,
        @RequestParam(required = false) datum: String?,
    ): ResponseEntity<List<Map<String, Any>>> {
        val date = if (datum != null) LocalDate.parse(datum) else LocalDate.now()
        return ResponseEntity.ok(service.getBuchungenByDatum(token, date))
    }

    @GetMapping("/feiertage")
    fun getFeiertage(@RequestParam jahr: Int): ResponseEntity<List<Map<String, Any>>> =
        ResponseEntity.ok(service.getFeiertage(jahr))

    @GetMapping("/urlaubsverfall/{token}")
    fun getUrlaubsverfallWarnung(@PathVariable token: String): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(service.getUrlaubsverfallWarnung(token))
}
