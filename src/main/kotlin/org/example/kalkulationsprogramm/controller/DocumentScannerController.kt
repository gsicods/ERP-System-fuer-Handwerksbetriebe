package org.example.kalkulationsprogramm.controller

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.example.kalkulationsprogramm.service.GeminiScannerService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import kotlin.math.min

@RestController
@RequestMapping("/api/documents")
class DocumentScannerController(
    private val geminiService: GeminiScannerService
) {
    @PostMapping("/scan-manual")
    fun scanManual(@RequestParam("image") imageFile: MultipartFile): ResponseEntity<ByteArray> =
        try {
            log.info("Received MANUAL scan request: {} bytes", imageFile.size)
            val imageBytes = imageFile.bytes
            val filename = geminiService.generateFilename(imageBytes)
            log.info("Generated filename: {}", filename)
            val pdfBytes = createPdfFromImage(imageBytes)
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes)
        } catch (e: Exception) {
            log.error("Manual Scan failed", e)
            ResponseEntity.internalServerError().build()
        }

    private fun createPdfFromImage(imageBytes: ByteArray): ByteArray =
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            val image = PDImageXObject.createFromByteArray(doc, imageBytes, "scan")
            PDPageContentStream(doc, page).use { contentStream ->
                val scale = min(PDRectangle.A4.width / image.width, PDRectangle.A4.height / image.height)
                contentStream.drawImage(image, 0f, 0f, image.width * scale, image.height * scale)
            }
            val out = ByteArrayOutputStream()
            doc.save(out)
            out.toByteArray()
        }

    companion object {
        private val log = LoggerFactory.getLogger(DocumentScannerController::class.java)
    }
}
