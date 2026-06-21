package org.example.kalkulationsprogramm.controller

import jakarta.validation.Valid
import org.example.kalkulationsprogramm.domain.Dokumenttyp
import org.example.kalkulationsprogramm.domain.TextbausteinTyp
import org.example.kalkulationsprogramm.dto.Textbaustein.TextbausteinDto
import org.example.kalkulationsprogramm.service.TextbausteinService
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
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
@RequestMapping("/api/textbausteine")
class TextbausteinController(
    private val textbausteinService: TextbausteinService
) {
    @GetMapping
    fun list(@RequestParam(value = "typ", required = false) typ: String?): ResponseEntity<List<TextbausteinDto>> =
        ResponseEntity.ok(textbausteinService.list(typ).map(TextbausteinDto::fromEntity))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<TextbausteinDto> =
        try {
            ResponseEntity.ok(TextbausteinDto.fromEntity(textbausteinService.get(id)))
        } catch (_: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }

    @PostMapping
    fun create(@Valid @RequestBody dto: TextbausteinDto): ResponseEntity<TextbausteinDto> =
        ResponseEntity.ok(TextbausteinDto.fromEntity(textbausteinService.create(dto)))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody dto: TextbausteinDto): ResponseEntity<TextbausteinDto> =
        try {
            ResponseEntity.ok(TextbausteinDto.fromEntity(textbausteinService.update(id, dto)))
        } catch (_: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        textbausteinService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/placeholders")
    fun placeholders(): ResponseEntity<List<Map<String, String>>> =
        ResponseEntity.ok(
            PLACEHOLDER_BESCHREIBUNG.entries
                .sortedBy { it.key }
                .map { mapOf("token" to it.key, "beschreibung" to it.value) }
        )

    @GetMapping("/typen")
    fun typen(): ResponseEntity<List<String>> =
            ResponseEntity.ok(
            listOf(TextbausteinTyp.VORTEXT, TextbausteinTyp.NACHTEXT, TextbausteinTyp.ZAHLUNGSZIEL, TextbausteinTyp.FREITEXT)
                .map { StringUtils.capitalize(it.name.lowercase()) }
        )

    @GetMapping("/dokumenttypen")
    fun dokumenttypen(): ResponseEntity<List<String>> =
        ResponseEntity.ok(Dokumenttyp.entries.map { it.label })

    companion object {
        private val PLACEHOLDER_BESCHREIBUNG = mapOf(
            "{{DOKUMENTNUMMER}}" to "Aktuelle Dokumentnummer",
            "{{PROJEKTNUMMER}}" to "Projektnummer",
            "{{Anrede}}" to "Anrede des Kunden",
            "{{KUNDENNAME}}" to "Kundenname",
            "{{Ansprechpartner}}" to "Ansprechpartner des Kunden",
            "{{KUNDENNUMMER}}" to "Kundennummer",
            "{{KUNDENADRESSE}}" to "Kundenadresse mehrzeilig",
            "{{BAUVORHABEN}}" to "Bauvorhaben / Projektname",
            "{{DATUM}}" to "Heutiges Datum",
            "{{SEITENZAHL}}" to "Seitenangabe",
            "{{DOKUMENTTYP}}" to "Dokumenttyp (z. B. Rechnung)",
            "{{ZAHLUNGSZIEL}}" to "Zahlungsziel als Datum",
            "{{ZAHLUNGSZIEL_TAGE}}" to "Zahlungsziel des Kunden in Tagen",
            "{{BEZUGSDOKUMENTNUMMER}}" to "Dokumentnummer des Bezugsdokuments (Vorgaenger)",
            "{{BEZUGSDOKUMENTDATUM}}" to "Datum des Bezugsdokuments (Vorgaenger)",
            "{{BEZUGSDOKUMENTTYP}}" to "Typ des Bezugsdokuments (z. B. Angebot)"
        )
    }
}
