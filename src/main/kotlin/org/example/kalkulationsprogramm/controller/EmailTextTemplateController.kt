package org.example.kalkulationsprogramm.controller

import jakarta.validation.Valid
import org.example.kalkulationsprogramm.domain.Dokumenttyp
import org.example.kalkulationsprogramm.dto.Email.EmailTextTemplateDto
import org.example.kalkulationsprogramm.service.EmailTextTemplateKategorien
import org.example.kalkulationsprogramm.service.EmailTextTemplateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/email-textvorlagen")
class EmailTextTemplateController(
    private val service: EmailTextTemplateService
) {
    @GetMapping
    fun list(): ResponseEntity<List<EmailTextTemplateDto>> =
        ResponseEntity.ok(service.list().map(EmailTextTemplateDto::fromEntity))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<EmailTextTemplateDto> =
        try {
            ResponseEntity.ok(EmailTextTemplateDto.fromEntity(service.get(id)))
        } catch (_: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }

    @PostMapping
    fun create(@Valid @RequestBody dto: EmailTextTemplateDto): ResponseEntity<EmailTextTemplateDto> =
        ResponseEntity.ok(EmailTextTemplateDto.fromEntity(service.create(dto)))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody dto: EmailTextTemplateDto): ResponseEntity<EmailTextTemplateDto> =
        try {
            ResponseEntity.ok(EmailTextTemplateDto.fromEntity(service.update(id, dto)))
        } catch (_: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/dokumenttypen")
    fun dokumenttypen(): ResponseEntity<List<Map<String, String>>> = ResponseEntity.ok(DOKUMENT_TYPEN)

    @GetMapping("/placeholders")
    fun placeholders(): ResponseEntity<List<Map<String, String>>> = ResponseEntity.ok(PLACEHOLDERS)

    companion object {
        private val DOKUMENT_TYPEN: List<Map<String, String>> = buildDokumentTypen()

        private fun buildDokumentTypen(): List<Map<String, String>> {
            val list = ArrayList<Map<String, String>>()
            for (d in Dokumenttyp.entries) {
                val label = if (d == Dokumenttyp.ANGEBOT) "Anfrage / Angebot" else d.label
                list.add(buildOption(d.name, label))
            }
            list.add(buildOption("ZEICHNUNG", "Zeichnung / Entwurf"))
            list.add(buildOption("WEBSITE_ANFRAGE_BESTAETIGUNG", "Webseite - Anfragebestaetigung"))
            return list.toList()
        }

        private fun buildOption(value: String, label: String): Map<String, String> {
            val kat = EmailTextTemplateKategorien.kategorieFuer(value)
            val option = LinkedHashMap<String, String>()
            option["value"] = value
            option["label"] = label
            option["kategorie"] = kat.name
            option["kategorieLabel"] = kat.label
            return option
        }

        private val PLACEHOLDERS: List<Map<String, String>> = listOf(
            mapOf("token" to "{{ANREDE}}", "label" to "Anrede (z. B. Sehr geehrter Herr Mueller)"),
            mapOf("token" to "{{KUNDENNAME}}", "label" to "Kundenname (Firma / Name des Kunden)"),
            mapOf("token" to "{{ANSPRECHPARTNER}}", "label" to "Ansprechpartner (Person beim Kunden)"),
            mapOf("token" to "{{BAUVORHABEN}}", "label" to "Bauvorhaben"),
            mapOf("token" to "{{PROJEKTNUMMER}}", "label" to "Projektnummer"),
            mapOf("token" to "{{DOKUMENTNUMMER}}", "label" to "Dokumentnummer (Rechnung/Auftrag/Anfrage)"),
            mapOf("token" to "{{RECHNUNGSDATUM}}", "label" to "Rechnungsdatum"),
            mapOf("token" to "{{FAELLIGKEITSDATUM}}", "label" to "Faelligkeitsdatum"),
            mapOf("token" to "{{BETRAG}}", "label" to "Betrag (formatiert)"),
            mapOf("token" to "{{BENUTZER}}", "label" to "Sachbearbeiter / Benutzer"),
            mapOf("token" to "{{REVIEW_LINK}}", "label" to "Google-Bewertungs-Link"),
            mapOf("token" to "{{BANK}}", "label" to "Bank (aus Firmeneinstellungen)"),
            mapOf("token" to "{{IBAN}}", "label" to "IBAN (aus Firmeneinstellungen)"),
            mapOf("token" to "{{BIC}}", "label" to "BIC (aus Firmeneinstellungen)"),
            mapOf("token" to "{{NACHRICHT}}", "label" to "Nachricht aus dem Webseiten-Funnel"),
            mapOf("token" to "{{ANFRAGE_DATUM}}", "label" to "Anfrage-Datum (Webseite)"),
            mapOf("token" to "{{ANFRAGENUMMER}}", "label" to "Anfrage-Nummer (Webseite)")
        )
    }
}
