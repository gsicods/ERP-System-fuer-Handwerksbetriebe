package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.BwaUploadDto
import org.example.kalkulationsprogramm.service.BwaService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.MalformedURLException
import java.nio.file.Path

@RestController
@RequestMapping("/api/bwa")
class BwaController(
    private val bwaService: BwaService,
    @Value("\${file.mail-attachment-dir}") private val mailAttachmentDir: String
) {
    @GetMapping("/jahre")
    fun getAvailableYears(): ResponseEntity<List<Int>> = ResponseEntity.ok(bwaService.findAvailableYears())

    @GetMapping("/jahr/{jahr}")
    fun getByJahr(@PathVariable jahr: Int): ResponseEntity<List<BwaUploadDto>> =
        ResponseEntity.ok(bwaService.findByJahr(jahr))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<BwaUploadDto> {
        val dto = bwaService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/{id}/pdf")
    fun downloadPdf(@PathVariable id: Long): ResponseEntity<Resource> {
        val filename = bwaService.findStoredFilename(id).orElse(null) ?: return ResponseEntity.notFound().build()
        return try {
            val path = Path.of(mailAttachmentDir).resolve(filename).normalize()
            val resource: Resource = UrlResource(path.toUri())
            if (resource.exists()) {
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
                    .body(resource)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (_: MalformedURLException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        bwaService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
