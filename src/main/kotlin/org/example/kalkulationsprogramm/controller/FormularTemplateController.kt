package org.example.kalkulationsprogramm.controller

import jakarta.validation.Valid
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateAssetResponse
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateCopyRequest
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateDto
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateListDto
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateRenameRequest
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateSaveRequest
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateSelectionRequest
import org.example.kalkulationsprogramm.dto.Formular.FormularTemplateUpdateRequest
import org.example.kalkulationsprogramm.dto.Formular.FormularTextbausteinDefaultsDto
import org.example.kalkulationsprogramm.dto.Formular.FormularTextbausteinResolvedDto
import org.example.kalkulationsprogramm.service.FormularTemplateService
import org.example.kalkulationsprogramm.service.FormularTemplateService.NamedTemplateData
import org.example.kalkulationsprogramm.service.FormularTextbausteinDefaultService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files

@RestController
@Validated
class FormularTemplateController(
    private val formularTemplateService: FormularTemplateService,
    private val textbausteinDefaultService: FormularTextbausteinDefaultService,
) {
    @GetMapping("/api/formulare/template")
    fun getTemplate(@RequestParam(name = "preset", required = false) preset: String?): FormularTemplateDto {
        val defaultRequested = preset != null && "default".equals(preset.trim(), ignoreCase = true)
        return FormularTemplateDto().apply {
            html = if (defaultRequested) formularTemplateService.defaultTemplate() else formularTemplateService.loadTemplate()
            lastModified = if (defaultRequested) null else formularTemplateService.lastModifiedIso
            placeholders = formularTemplateService.supportedPlaceholders
            assignedDokumenttypen = emptyList()
            assignedUserIds = emptyList()
        }
    }

    @PutMapping("/api/formulare/template")
    fun saveTemplate(@Valid @RequestBody request: FormularTemplateUpdateRequest): FormularTemplateDto {
        val html = formularTemplateService.saveTemplate(request.html)
        return FormularTemplateDto().apply {
            this.html = html
            lastModified = formularTemplateService.lastModifiedIso
            placeholders = formularTemplateService.supportedPlaceholders
            assignedDokumenttypen = emptyList()
        }
    }

    @PostMapping(path = ["/api/formulare/template/logo"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadLogo(@RequestPart("file") file: MultipartFile): ResponseEntity<FormularTemplateAssetResponse> {
        val url = formularTemplateService.storeLogo(file)
        val filename = url.substring(url.lastIndexOf('/') + 1)
        return ResponseEntity.ok(FormularTemplateAssetResponse(url, filename))
    }

    @GetMapping("/api/formulare/template/assets/{filename:.+}")
    fun getAsset(@PathVariable filename: String): ResponseEntity<Resource> {
        val resource = formularTemplateService.loadAsset(filename)
        var contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
        try {
            contentType = Files.probeContentType(resource.file.toPath()) ?: contentType
        } catch (_: IOException) {
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${resource.filename}\"")
            .body(resource)
    }

    @GetMapping("/api/formulare/templates")
    fun listTemplates(): ResponseEntity<List<FormularTemplateListDto>> {
        return ResponseEntity.ok(formularTemplateService.listNamedTemplates())
    }

    @PostMapping("/api/formulare/templates")
    fun saveNamedTemplate(@Valid @RequestBody request: FormularTemplateSaveRequest): ResponseEntity<FormularTemplateDto> {
        val saved = formularTemplateService.saveNamedTemplate(request.name, request.html, request.assignedDokumenttypen)
        return ResponseEntity.ok(toDto(saved))
    }

    @PostMapping("/api/formulare/templates/{name}/copy")
    fun copyNamedTemplate(
        @PathVariable name: String,
        @Valid @RequestBody request: FormularTemplateCopyRequest,
    ): ResponseEntity<FormularTemplateDto> {
        return ResponseEntity.ok(toDto(formularTemplateService.copyNamedTemplate(name, request.newName)))
    }

    @GetMapping("/api/formulare/templates/{name}")
    fun loadNamedTemplate(@PathVariable name: String): ResponseEntity<FormularTemplateDto> {
        return ResponseEntity.ok(toDto(formularTemplateService.loadNamedTemplate(name)))
    }

    @PutMapping("/api/formulare/templates/{name}/rename")
    fun renameTemplate(
        @PathVariable name: String,
        @Valid @RequestBody request: FormularTemplateRenameRequest,
    ): ResponseEntity<FormularTemplateDto> {
        return ResponseEntity.ok(toDto(formularTemplateService.renameNamedTemplate(name, request.newName)))
    }

    @DeleteMapping("/api/formulare/templates/{name}")
    fun deleteNamedTemplate(@PathVariable name: String): ResponseEntity<Void> {
        formularTemplateService.deleteNamedTemplate(name)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/formulare/templates/selection")
    fun getTemplateSelection(
        @RequestParam dokumenttyp: String,
        @RequestParam(required = false) userId: Long?,
    ): ResponseEntity<String> {
        return formularTemplateService.getPreferredTemplateForDokumenttyp(dokumenttyp, userId)
            .map { ResponseEntity.ok(it) }
            .orElseGet { ResponseEntity.noContent().build() }
    }

    @PostMapping("/api/formulare/templates/selection")
    fun setTemplateSelection(@Valid @RequestBody request: FormularTemplateSelectionRequest): ResponseEntity<Void> {
        val userIds = if (!request.userIds.isNullOrEmpty()) {
            request.userIds
        } else if (request.userId != null) {
            listOf(request.userId)
        } else {
            emptyList()
        }
        formularTemplateService.setPreferredTemplateForDokumenttyp(request.dokumenttyp, request.templateName, userIds)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/formulare/placeholders")
    fun resolvePlaceholders(
        @RequestParam(required = false) projektId: Long?,
        @RequestParam(defaultValue = "false") generateDoknr: Boolean,
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(formularTemplateService.resolvePlaceholders(projektId, generateDoknr))
    }

    @PostMapping("/api/formulare/dokumentnummer")
    fun generateDokumentnummer(): ResponseEntity<String> {
        return ResponseEntity.ok(formularTemplateService.generateDokumentnummer())
    }

    @GetMapping("/api/formulare/templates/{name}/textbaustein-defaults")
    fun getTextbausteinDefaults(@PathVariable name: String): ResponseEntity<FormularTextbausteinDefaultsDto> {
        return ResponseEntity.ok(textbausteinDefaultService.loadDefaults(name))
    }

    @PutMapping("/api/formulare/templates/{name}/textbaustein-defaults")
    fun setTextbausteinDefaults(
        @PathVariable name: String,
        @RequestBody request: FormularTextbausteinDefaultsDto,
    ): ResponseEntity<FormularTextbausteinDefaultsDto> {
        textbausteinDefaultService.replaceDefaults(name, request)
        return ResponseEntity.ok(textbausteinDefaultService.loadDefaults(name))
    }

    @GetMapping("/api/formulare/templates/{name}/textbaustein-defaults/resolve")
    fun resolveTextbausteinDefaults(
        @PathVariable name: String,
        @RequestParam dokumenttyp: String,
    ): ResponseEntity<FormularTextbausteinResolvedDto> {
        val result = textbausteinDefaultService.loadForDokumenttyp(name, dokumenttyp)
        return ResponseEntity.ok(FormularTextbausteinResolvedDto.from(result.vortexte(), result.nachtexte()))
    }

    private fun toDto(data: NamedTemplateData): FormularTemplateDto {
        return FormularTemplateDto().apply {
            name = data.name()
            html = data.html()
            placeholders = formularTemplateService.supportedPlaceholders
            assignedDokumenttypen = data.assignedDokumenttypen()
            assignedUserIds = data.assignedUserIds()
            created = data.created()
            modified = data.modified()
            lastModified = data.modified()
        }
    }
}
