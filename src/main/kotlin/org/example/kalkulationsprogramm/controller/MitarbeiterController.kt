package org.example.kalkulationsprogramm.controller

import jakarta.validation.Valid
import org.example.kalkulationsprogramm.domain.DokumentGruppe
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterDokumentResponseDto
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterDto
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterErstellenDto
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterNotizDto
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterStundenlohnDto
import org.example.kalkulationsprogramm.service.MitarbeiterService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/mitarbeiter")
class MitarbeiterController(
    private val service: MitarbeiterService,
) {
    @GetMapping
    fun list(): ResponseEntity<List<MitarbeiterDto>> = ResponseEntity.ok(service.list())

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<MitarbeiterDto> =
        service.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @PostMapping
    fun create(@RequestBody dto: MitarbeiterErstellenDto): ResponseEntity<MitarbeiterDto> =
        ResponseEntity.ok(service.save(null, dto))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dto: MitarbeiterErstellenDto): ResponseEntity<MitarbeiterDto> =
        ResponseEntity.ok(service.save(id, dto))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> =
        try {
            service.delete(id)
            ResponseEntity.noContent().build()
        } catch (_: Exception) {
            ResponseEntity.notFound().build()
        }

    @PostMapping(value = ["/{id}/dokumente"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadDokument(
        @PathVariable id: Long,
        @RequestParam("datei") datei: MultipartFile,
        @RequestParam(value = "gruppe", required = false) gruppe: DokumentGruppe?,
    ): ResponseEntity<MitarbeiterDokumentResponseDto> =
        ResponseEntity.ok(service.uploadDokument(id, datei, gruppe))

    @GetMapping("/{id}/dokumente")
    fun listDokumente(@PathVariable id: Long): ResponseEntity<List<MitarbeiterDokumentResponseDto>> =
        ResponseEntity.ok(service.listDokumente(id))

    @GetMapping(value = ["/{id}/qr-code"], produces = [MediaType.IMAGE_PNG_VALUE])
    fun getQrCode(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "300") width: Int,
        @RequestParam(defaultValue = "300") height: Int,
    ): ResponseEntity<ByteArray> =
        try {
            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(service.generateQrCode(id, width, height))
        } catch (_: RuntimeException) {
            ResponseEntity.notFound().build()
        }

    @PostMapping("/{id}/regenerate-token")
    fun regenerateToken(@PathVariable id: Long): ResponseEntity<String> =
        try {
            ResponseEntity.ok(service.generateLoginToken(id))
        } catch (_: RuntimeException) {
            ResponseEntity.notFound().build()
        }

    @GetMapping("/by-token/{token}")
    fun getByToken(@PathVariable token: String): ResponseEntity<MitarbeiterDto> =
        service.findByToken(token)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @GetMapping("/{id}/notizen")
    fun listNotizen(@PathVariable id: Long): ResponseEntity<List<MitarbeiterNotizDto>> =
        ResponseEntity.ok(service.listNotizen(id))

    @PostMapping("/{id}/notizen")
    fun createNotiz(@PathVariable id: Long, @RequestBody inhalt: String): ResponseEntity<MitarbeiterNotizDto> =
        ResponseEntity.ok(service.createNotiz(id, inhalt))

    @DeleteMapping("/notizen/{notizId}")
    fun deleteNotiz(@PathVariable notizId: Long): ResponseEntity<Void> {
        service.deleteNotiz(notizId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/stundenlohn-historie")
    fun listStundenloehne(@PathVariable id: Long): ResponseEntity<List<MitarbeiterStundenlohnDto>> =
        ResponseEntity.ok(service.listStundenloehne(id))

    @PostMapping("/{id}/stundenlohn-historie")
    fun addStundenlohn(
        @PathVariable id: Long,
        @Valid @RequestBody dto: MitarbeiterStundenlohnDto,
    ): ResponseEntity<*> =
        try {
            ResponseEntity.ok(service.addStundenlohn(id, dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }

    @PutMapping("/stundenlohn-historie/{eintragId}")
    fun updateStundenlohn(
        @PathVariable eintragId: Long,
        @Valid @RequestBody dto: MitarbeiterStundenlohnDto,
    ): ResponseEntity<*> =
        try {
            ResponseEntity.ok(service.updateStundenlohn(eintragId, dto))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        }

    @DeleteMapping("/stundenlohn-historie/{eintragId}")
    fun deleteStundenlohn(@PathVariable eintragId: Long): ResponseEntity<Void> {
        service.deleteStundenlohn(eintragId)
        return ResponseEntity.noContent().build()
    }
}
