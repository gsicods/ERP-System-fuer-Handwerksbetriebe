package org.example.kalkulationsprogramm.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestController
@RequestMapping("/api/system")
class SystemUtilityController {
    @Value("\${app.downloads.path:downloads}")
    private lateinit var downloadsPath: String

    @GetMapping("/openfile-launcher-setup")
    fun downloadOpenFileLauncherSetup(): ResponseEntity<ByteArray> {
        val downloadsDir = Path.of(downloadsPath)
        val batFile = downloadsDir.resolve("OpenFileLauncher-Install.bat")
        val ps1File = downloadsDir.resolve("OpenFileLauncher-Setup.ps1")
        if (!Files.exists(batFile) || !Files.exists(ps1File)) return ResponseEntity.notFound().build()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            addToZip(zos, batFile, "OpenFileLauncher-Install.bat")
            addToZip(zos, ps1File, "OpenFileLauncher-Setup.ps1")
        }
        val zipContent = baos.toByteArray()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.setContentDispositionFormData("attachment", "OpenFileLauncher-Setup.zip")
        headers.contentLength = zipContent.size.toLong()
        return ResponseEntity.ok().headers(headers).body(zipContent)
    }

    private fun addToZip(zos: ZipOutputStream, file: Path, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        Files.copy(file, zos)
        zos.closeEntry()
    }
}
