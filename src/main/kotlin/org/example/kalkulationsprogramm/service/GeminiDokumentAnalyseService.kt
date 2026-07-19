package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.LieferantDokument
import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp
import org.example.kalkulationsprogramm.domain.LieferantGeschaeftsdokument
import org.example.kalkulationsprogramm.domain.Lieferanten
import org.example.kalkulationsprogramm.dto.LieferantDokumentDto
import org.example.kalkulationsprogramm.dto.Zugferd.ZugferdDaten
import org.example.kalkulationsprogramm.repository.LieferantDokumentRepository
import org.example.kalkulationsprogramm.repository.LieferantGeschaeftsdokumentRepository
import org.example.kalkulationsprogramm.repository.LieferantenRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Optional
import java.util.concurrent.locks.ReentrantLock

@Service
open class GeminiDokumentAnalyseService(
    private val objectMapper: ObjectMapper,
    private val lieferantenRepository: LieferantenRepository,
    private val dokumentRepository: LieferantDokumentRepository,
    private val lieferantGeschaeftsdokumentRepository: LieferantGeschaeftsdokumentRepository,
    private val zugferdExtractorService: ZugferdExtractorService,
    private val systemSettingsService: SystemSettingsService,
    @Value("\${upload.path:uploads}")
    private val uploadPath: String,
    @Value("\${ai.gemini.model.dokument-analyse:gemini-flash-latest}")
    private val geminiModel: String,
    @Value("\${ai.gemini.model.pro:gemini-pro-latest}")
    private val geminiProModel: String,
) {
    @Transactional
    open fun analysiereDokument(dokument: LieferantDokument): LieferantGeschaeftsdokument? =
        analysiereDokument(dokument, null)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun reanalysiereDokumentById(dokumentId: Long): LieferantGeschaeftsdokument? {
        val dokument = dokumentRepository.findById(dokumentId).orElse(null) ?: run {
            log.warn("Dokument {} nicht gefunden fuer Re-Analyse", dokumentId)
            return null
        }
        return analysiereDokument(dokument, null)
    }

    fun analyzeAsync(dokumentId: Long) {
        reanalysiereDokumentById(dokumentId)
    }
    fun analyzeFile(file: MultipartFile): LieferantDokumentDto.AnalyzeResponse = analyzeFile(file, null)

    fun analyzeFile(file: MultipartFile, customPrompt: String?): LieferantDokumentDto.AnalyzeResponse {
        val suffix = file.originalFilename?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ".tmp"
        val tempFile = Files.createTempFile("lieferant-analyse-", suffix)
        return try {
            file.inputStream.use { input -> Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
            analyzeFile(tempFile, file.originalFilename)
        } finally {
            runCatching { Files.deleteIfExists(tempFile) }
        }
    }

    fun analyzeFile(file: Path, originalDateiname: String?): LieferantDokumentDto.AnalyzeResponse =
        analyzeFile(file, originalDateiname, false)

    fun analyzeFile(file: Path, originalDateiname: String?, useProModel: Boolean): LieferantDokumentDto.AnalyzeResponse {
        val validPath = validiereAnalyseDateiPfad(file, true)
        val name = originalDateiname ?: file.fileName?.toString().orEmpty()
        analyzeAndReturnData(validPath, name)?.let { return toAnalyzeResponse(it) }
        return analysierePerKiFuerPreview(validPath, name, useProModel) ?: LieferantDokumentDto.AnalyzeResponse()
    }

    fun analyzeFileForMultipleInvoices(file: MultipartFile): MutableList<LieferantDokumentDto.MultiInvoiceAnalyzeResponse> {
        val suffix = file.originalFilename?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ".tmp"
        val tempFile = Files.createTempFile("lieferant-multi-analyse-", suffix)
        return try {
            file.inputStream.use { input -> Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
            analyzeFileForMultipleInvoices(tempFile, file.originalFilename)
        } finally {
            runCatching { Files.deleteIfExists(tempFile) }
        }
    }

    fun analyzeFileForMultipleInvoices(file: Path, originalDateiname: String?): MutableList<LieferantDokumentDto.MultiInvoiceAnalyzeResponse> {
        val results = mutableListOf<LieferantDokumentDto.MultiInvoiceAnalyzeResponse>()
        val validPath = validiereAnalyseDateiPfad(file, true)
        val name = originalDateiname ?: file.fileName?.toString().orEmpty()
        val bytes = Files.readAllBytes(validPath)

        if (!name.lowercase().endsWith(".pdf")) {
            results += LieferantDokumentDto.MultiInvoiceAnalyzeResponse.builder()
                .pageRange("alle")
                .analyzeResponse(analyzeFile(validPath, name))
                .build()
            return results
        }

        val jsonResponse = rufGeminiApiMitPrompt(bytes, "application/pdf", buildMultiDocumentPrompt())
        if (!jsonResponse.isNullOrBlank()) {
            runCatching {
                val json = objectMapper.readTree(jsonResponse)
                if (json.isArray && json.size() > 0) {
                    Loader.loadPDF(validPath.toFile()).use { originalDoc ->
                        for (node in json) {
                            val pageRange = node.path("seiten").asText("alle")
                            val pages = parsePageRange(pageRange, originalDoc.numberOfPages)
                            val splitBytes = extractPages(originalDoc, pages)
                            results += LieferantDokumentDto.MultiInvoiceAnalyzeResponse.builder()
                                .pageRange(pageRange)
                                .analyzeResponse(parseJsonToAnalyzeResponse(node.toString()) ?: LieferantDokumentDto.AnalyzeResponse())
                                .splitPdfBase64(Base64.getEncoder().encodeToString(splitBytes))
                                .build()
                        }
                    }
                }
            }.onFailure { log.warn("Multi-Invoice JSON konnte nicht verarbeitet werden: {}", it.message) }
        }

        if (results.isEmpty()) {
            results += LieferantDokumentDto.MultiInvoiceAnalyzeResponse.builder()
                .pageRange("alle")
                .analyzeResponse(analyzeFile(validPath, name))
                .splitPdfBase64(Base64.getEncoder().encodeToString(bytes))
                .build()
        }
        return results
    }

    fun rufGeminiApiMitPrompt(bytes: ByteArray, mimeType: String?, customPrompt: String?): String? =
        rufGeminiApiMitPrompt(bytes, mimeType, customPrompt, false)

    fun rufGeminiApiMitPrompt(bytes: ByteArray, mimeType: String?, customPrompt: String?, useProModel: Boolean): String? {
        apiLock.lock()
        try {
            throttleGeminiCalls()
            val apiKey = systemSettingsService.geminiApiKey
            if (apiKey.isBlank() || apiKey == "OVERRIDE_IN_LOCAL" || apiKey == "DEIN_API_KEY_HIER") {
                log.warn("Gemini API Key nicht konfiguriert")
                return null
            }

            val model = if (useProModel) geminiProModel else geminiModel
            val requestJson = objectMapper.createObjectNode().apply {
                putArray("contents").addObject().apply {
                    putArray("parts").apply {
                        addObject().putObject("inline_data").apply {
                            put("mime_type", mimeType ?: "application/pdf")
                            put("data", Base64.getEncoder().encodeToString(bytes))
                        }
                        addObject().put("text", customPrompt ?: "Analysiere dieses Dokument und antworte nur mit JSON.")
                    }
                }
                putObject("generationConfig").apply {
                    put("temperature", 0.0)
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", 16384)
                }
            }

            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(180))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestJson), StandardCharsets.UTF_8))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            lastApiCallTime = System.currentTimeMillis()
            if (response.statusCode() !in 200..299) {
                log.warn("Gemini API Fehler {}: {}", response.statusCode(), response.body().take(500))
                return null
            }
            return extractGeminiText(response.body())
        } catch (e: Exception) {
            log.warn("Gemini API-Aufruf fehlgeschlagen: {}", e.message)
            return null
        } finally {
            apiLock.unlock()
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun analysiereDokument(dokument: LieferantDokument, explicitPath: Path?): LieferantGeschaeftsdokument? {
        val dokumentId = dokument.id ?: return null
        val freshDokument = dokumentRepository.findById(dokumentId).orElse(null) ?: run {
            log.warn("Dokument {} nicht gefunden fuer Analyse", dokumentId)
            return null
        }

        val dateiPfad = explicitPath?.let { validiereAnalyseDateiPfad(it, true) } ?: getDateiPfad(freshDokument)
        if (dateiPfad == null) {
            log.warn("Konnte Datei nicht finden fuer Dokument {}", freshDokument.id)
            return null
        }

        val geschaeftsdaten = analyzeAndReturnData(dateiPfad, freshDokument.getEffektiverDateiname())
            ?: analysierePerKi(dateiPfad, freshDokument.getEffektiverDateiname()) ?: run {
            log.warn("Konnte keine strukturierten Metadaten extrahieren fuer Dokument {}", freshDokument.id)
            return null
        }

        geschaeftsdaten.dokument = freshDokument
        geschaeftsdaten.id = freshDokument.id
        geschaeftsdaten.analysiertAm = LocalDateTime.now()

        val typ = geschaeftsdaten.detectedTyp ?: erkenneTypAusNummer(geschaeftsdaten.dokumentNummer)
        if (typ != null && (freshDokument.typ == null || freshDokument.typ == LieferantDokumentTyp.SONSTIG)) {
            freshDokument.typ = typ
        }

        automatischeVerknuepfung(freshDokument, geschaeftsdaten)

        val saved = lieferantGeschaeftsdokumentRepository.saveAndFlush(geschaeftsdaten)
        freshDokument.geschaeftsdaten = saved
        dokumentRepository.saveAndFlush(freshDokument)
        performRelink(freshDokument)

        log.info("Dokument {} erfolgreich strukturiert analysiert: {}", freshDokument.id, saved.dokumentNummer)
        return saved
    }

    fun analyzeAndReturnData(dateiPfad: Path, originalDateiname: String?): LieferantGeschaeftsdokument? {
        val name = originalDateiname ?: dateiPfad.fileName?.toString()
        val lower = name.orEmpty().lowercase()
        if (lower.endsWith(".xml")) {
            return parseXmlToGeschaeftsdokument(Files.readString(validiereAnalyseDateiPfad(dateiPfad, true), StandardCharsets.UTF_8))
        }
        if (!lower.endsWith(".pdf")) return null

        val zugferdDaten = runCatching { zugferdExtractorService.extract(dateiPfad.toString(), name) }
            .onFailure { log.info("ZUGFeRD-Analyse fehlgeschlagen fuer {}: {}", name, it.message) }
            .getOrNull() ?: return null

        if (!zugferdDaten.hasUsefulData()) {
            return null
        }

        return toGeschaeftsdokument(zugferdDaten)
    }

    private fun getDateiPfad(dokument: LieferantDokument): Path? {
        val dateiname = dokument.getEffektiverGespeicherterDateiname() ?: return null
        val lieferantId = dokument.lieferant?.id

        val kandidaten = listOfNotNull(
            Path.of(uploadPath, "attachments", dateiname),
            lieferantId?.let { Path.of(uploadPath, "attachments", "lieferanten", it.toString(), dateiname) },
            lieferantId?.let { Path.of(uploadPath, "lieferanten", it.toString(), dateiname) },
            Path.of(uploadPath, "attachments", "vendor-invoices", dateiname),
            Path.of(uploadPath, "lieferant-emails", dateiname),
            Path.of(uploadPath, "email", dateiname),
            Path.of(uploadPath, dateiname),
        )

        return kandidaten.firstNotNullOfOrNull(::pruefeUploadDateiPfad)
    }

    private fun pruefeUploadDateiPfad(pfad: Path): Path? {
        val normalizedPath = pfad.toAbsolutePath().normalize()
        val uploadRoot = uploadRootPath()
        if (!normalizedPath.startsWith(uploadRoot) || !Files.isRegularFile(normalizedPath)) {
            return null
        }
        return normalizedPath
    }

    private fun validiereAnalyseDateiPfad(dateiPfad: Path, tempDateienErlaubt: Boolean): Path {
        val normalizedPath = dateiPfad.toAbsolutePath().normalize()
        val uploadRoot = uploadRootPath()
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()
        val erlaubterPfad = normalizedPath.startsWith(uploadRoot) || (tempDateienErlaubt && normalizedPath.startsWith(tempRoot))
        require(erlaubterPfad) { "Dateizugriff ausserhalb der erlaubten Verzeichnisse: $normalizedPath" }
        require(Files.isRegularFile(normalizedPath)) { "Datei ist nicht lesbar oder existiert nicht: $normalizedPath" }
        return normalizedPath
    }

    private fun uploadRootPath(): Path =
        Path.of(uploadPath.ifBlank { "uploads" }).toAbsolutePath().normalize()

    private fun analysierePerKiFuerPreview(
        dateiPfad: Path,
        originalFilename: String?,
        useProModel: Boolean,
    ): LieferantDokumentDto.AnalyzeResponse? {
        val bytes = Files.readAllBytes(dateiPfad)
        val jsonResponse = rufGeminiApiMitPrompt(bytes, getMimeTypeFromFilename(originalFilename), dokumentAnalysePrompt(), useProModel)
            ?: return null
        return parseJsonToAnalyzeResponse(jsonResponse)
    }

    private fun analysierePerKi(dateiPfad: Path, originalFilename: String?): LieferantGeschaeftsdokument? {
        val bytes = Files.readAllBytes(dateiPfad)
        val jsonResponse = rufGeminiApiMitPrompt(bytes, getMimeTypeFromFilename(originalFilename), dokumentAnalysePrompt(), false)
            ?: return null
        return parseJsonToGeschaeftsdokument(jsonResponse)
    }

    private fun parseJsonToAnalyzeResponse(jsonResponse: String): LieferantDokumentDto.AnalyzeResponse? =
        runCatching {
            val json = objectMapper.readTree(jsonResponse)
            LieferantDokumentDto.AnalyzeResponse.builder()
                .dokumentTyp(parseDokumentTyp(json.path("dokumentTyp").asText(null)) ?: LieferantDokumentTyp.RECHNUNG)
                .dokumentNummer(textOrNull(json, "dokumentNummer"))
                .dokumentDatum(parseDateFlexibel(textOrNull(json, "dokumentDatum")))
                .betragNetto(decimalOrNull(json, "betragNetto"))
                .betragBrutto(decimalOrNull(json, "betragBrutto"))
                .mwstSatz(decimalOrNull(json, "mwstSatz"))
                .liefertermin(parseDateFlexibel(textOrNull(json, "liefertermin")))
                .zahlungsziel(
                    parseDateFlexibel(textOrNull(json, "zahlungsziel"))
                        ?: parseDateFlexibel(textOrNull(json, "dokumentDatum"))?.let { datum ->
                            intOrNull(json, "nettoTage")?.takeIf { it > 0 }?.let { datum.plusDays(it.toLong()) }
                        },
                )
                .bestellnummer(textOrNull(json, "bestellnummer"))
                .referenzNummer(textOrNull(json, "referenzNummer"))
                .skontoTage(intOrNull(json, "skontoTage"))
                .skontoProzent(decimalOrNull(json, "skontoProzent"))
                .nettoTage(intOrNull(json, "nettoTage"))
                .bereitsGezahlt(booleanOrNull(json, "bereitsGezahlt"))
                .zahlungsart(normalizeZahlungsart(textOrNull(json, "zahlungsart")))
                .aiConfidence(doubleOrNull(json, "confidence") ?: 0.8)
                .analyseQuelle("KI")
                .lieferantName(textOrNull(json, "lieferantName"))
                .lieferantStrasse(textOrNull(json, "lieferantStrasse"))
                .lieferantPlz(textOrNull(json, "lieferantPlz"))
                .lieferantOrt(textOrNull(json, "lieferantOrt"))
                .build()
        }.onFailure { log.warn("Fehler beim Parsen der KI-Antwort: {}", it.message) }.getOrNull()

    private fun parseJsonToGeschaeftsdokument(jsonResponse: String): LieferantGeschaeftsdokument? =
        runCatching {
            val json = objectMapper.readTree(jsonResponse)
            if (json.has("istGeschaeftsdokument") && !json.path("istGeschaeftsdokument").asBoolean(true)) return null
            val typ = parseDokumentTyp(json.path("dokumentTyp").asText(null))
            if (typ == LieferantDokumentTyp.SONSTIG) return null
            LieferantGeschaeftsdokument().apply {
                aiRawJson = jsonResponse
                analysiertAm = LocalDateTime.now()
                detectedTyp = typ
                dokumentNummer = textOrNull(json, "dokumentNummer")
                dokumentDatum = parseDateFlexibel(textOrNull(json, "dokumentDatum"))
                betragNetto = decimalOrNull(json, "betragNetto")
                betragBrutto = decimalOrNull(json, "betragBrutto")
                mwstSatz = decimalOrNull(json, "mwstSatz")
                liefertermin = parseDateFlexibel(textOrNull(json, "liefertermin"))
                bestellnummer = textOrNull(json, "bestellnummer")
                referenzNummer = textOrNull(json, "referenzNummer")
                nettoTage = intOrNull(json, "nettoTage")
                zahlungsziel = parseDateFlexibel(textOrNull(json, "zahlungsziel"))
                    ?: dokumentDatum?.let { datum -> nettoTage?.takeIf { it > 0 }?.let { datum.plusDays(it.toLong()) } }
                skontoTage = intOrNull(json, "skontoTage")
                skontoProzent = decimalOrNull(json, "skontoProzent")
                bereitsGezahlt = booleanOrNull(json, "bereitsGezahlt") ?: false
                if (bereitsGezahlt == true) bezahlt = true
                zahlungsart = normalizeZahlungsart(textOrNull(json, "zahlungsart"))
                aiConfidence = doubleOrNull(json, "confidence") ?: 0.8
                datenquelle = "KI"
                manuellePruefungErforderlich = (aiConfidence ?: 0.0) < 0.7
            }
        }.onFailure { log.warn("KI-Daten konnten nicht in Geschaeftsdokument gemappt werden: {}", it.message) }.getOrNull()

    private fun parseXmlToGeschaeftsdokument(xml: String): LieferantGeschaeftsdokument? {
        if (!xml.contains("Invoice", ignoreCase = true) && !xml.contains("CrossIndustryInvoice", ignoreCase = true)) return null
        val nummer = extractXmlValue(xml, "ID", "InvoiceNumber", "ram:ID")
        val brutto = extractXmlValue(xml, "GrandTotalAmount", "PayableAmount", "ram:GrandTotalAmount")?.toDecimalOrNull()
        if (nummer == null && brutto == null) return null
        val rawDate = extractXmlValue(xml, "IssueDate", "IssueDateTime", "ram:DateTimeString")
        return LieferantGeschaeftsdokument().apply {
            detectedTyp = LieferantDokumentTyp.RECHNUNG
            dokumentNummer = nummer
            dokumentDatum = parseDateFlexibel(rawDate?.replace(Regex("[^0-9.]"), ""))
            betragBrutto = brutto
            aiConfidence = 1.0
            datenquelle = "XML"
            analysiertAm = LocalDateTime.now()
        }
    }

    private fun extractXmlValue(xml: String, vararg tagNames: String): String? {
        for (tag in tagNames) {
            val cleanTag = Regex.escape(tag)
            val patterns = listOf(
                Regex("<$cleanTag[^>]*>(.*?)</$cleanTag>", RegexOption.IGNORE_CASE),
                Regex("<[^>]*:?$cleanTag[^>]*>(.*?)</[^>]*:?$cleanTag>", RegexOption.IGNORE_CASE),
            )
            for (pattern in patterns) {
                val match = pattern.find(xml)
                if (match != null) return match.groupValues[1].trim().takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun extractGeminiText(responseBody: String): String? {
        val root = objectMapper.readTree(responseBody)
        val text = root.path("candidates").firstOrNull()
            ?.path("content")
            ?.path("parts")
            ?.firstOrNull()
            ?.path("text")
            ?.asText()
            ?.trim()
            ?: return null
        return stripMarkdownJsonFence(text)
    }

    private fun stripMarkdownJsonFence(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) return trimmed
        val withoutStart = trimmed.substringAfter('\n', "")
        return withoutStart.substringBeforeLast("```").trim().ifBlank { trimmed }
    }

    private fun parsePageRange(pageRange: String?, totalPages: Int): IntArray {
        if (pageRange.isNullOrBlank() || pageRange.equals("alle", ignoreCase = true)) return IntArray(totalPages) { it }
        return runCatching {
            pageRange.split(",").flatMap { part ->
                val trimmed = part.trim()
                if (trimmed.contains("-")) {
                    val start = trimmed.substringBefore("-").trim().toInt() - 1
                    val end = trimmed.substringAfter("-").trim().toInt() - 1
                    (start..end).filter { it in 0 until totalPages }
                } else {
                    listOf(trimmed.toInt() - 1).filter { it in 0 until totalPages }
                }
            }.toIntArray().takeIf { it.isNotEmpty() } ?: IntArray(totalPages) { it }
        }.getOrElse {
            log.warn("Konnte Page-Range nicht parsen: {}", pageRange)
            IntArray(totalPages) { it }
        }
    }

    private fun extractPages(sourceDoc: PDDocument, pageIndices: IntArray): ByteArray =
        PDDocument().use { newDoc ->
            pageIndices.forEach { pageIndex ->
                if (pageIndex in 0 until sourceDoc.numberOfPages) {
                    newDoc.addPage(sourceDoc.getPage(pageIndex))
                }
            }
            ByteArrayOutputStream().use { out ->
                newDoc.save(out)
                out.toByteArray()
            }
        }

    private fun throttleGeminiCalls() {
        val elapsed = System.currentTimeMillis() - lastApiCallTime
        if (lastApiCallTime > 0 && elapsed < API_CALL_DELAY_MS) {
            Thread.sleep(API_CALL_DELAY_MS - elapsed)
        }
    }

    private fun getMimeTypeFromFilename(filename: String?): String =
        when {
            filename?.lowercase()?.endsWith(".jpg") == true || filename?.lowercase()?.endsWith(".jpeg") == true -> "image/jpeg"
            filename?.lowercase()?.endsWith(".png") == true -> "image/png"
            filename?.lowercase()?.endsWith(".xml") == true -> "application/xml"
            else -> "application/pdf"
        }

    private fun parseDateFlexibel(value: String?): LocalDate? {
        val text = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val candidates = listOf(
            { LocalDate.parse(text) },
            { LocalDate.parse(text, DateTimeFormatter.ofPattern("dd.MM.yyyy")) },
            { LocalDate.parse(text, DateTimeFormatter.ofPattern("dd/MM/yyyy")) },
            { if (text.matches(Regex("\\d{8}"))) LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE) else null },
        )
        for (candidate in candidates) {
            runCatching { candidate() }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun parseDokumentTyp(value: String?): LieferantDokumentTyp? {
        val normalized = value?.replace(" (Kopie)", "")?.trim()?.uppercase() ?: return null
        return runCatching { LieferantDokumentTyp.valueOf(normalized) }.getOrElse {
            when {
                normalized.contains("RECHNUNG") -> LieferantDokumentTyp.RECHNUNG
                normalized.contains("AUFTRAG") -> LieferantDokumentTyp.AUFTRAGSBESTAETIGUNG
                normalized.contains("LIEFER") -> LieferantDokumentTyp.LIEFERSCHEIN
                normalized.contains("ANGEBOT") -> LieferantDokumentTyp.ANGEBOT
                normalized.contains("GUTSCHRIFT") -> LieferantDokumentTyp.GUTSCHRIFT
                normalized.contains("SONSTIG") -> LieferantDokumentTyp.SONSTIG
                else -> null
            }
        }
    }

    private fun textOrNull(json: JsonNode, field: String): String? =
        json.path(field).takeUnless { it.isMissingNode || it.isNull }?.asText()?.trim()?.takeIf { it.isNotBlank() && it != "null" }

    private fun decimalOrNull(json: JsonNode, field: String): BigDecimal? = textOrNull(json, field)?.toDecimalOrNull()
    private fun intOrNull(json: JsonNode, field: String): Int? = textOrNull(json, field)?.toIntOrNull()
    private fun doubleOrNull(json: JsonNode, field: String): Double? = textOrNull(json, field)?.toDoubleOrNull()
    private fun booleanOrNull(json: JsonNode, field: String): Boolean? =
        json.path(field).takeUnless { it.isMissingNode || it.isNull }?.asBoolean()

    private fun String.toDecimalOrNull(): BigDecimal? =
        runCatching { BigDecimal(trim().replace(",", ".")) }.getOrNull()

    private fun normalizeZahlungsart(value: String?): String? {
        val normalized = value?.trim()?.uppercase()?.replace('-', '_')?.replace(' ', '_')?.replace(".", "") ?: return null
        return when (normalized) {
            "VORAUSKASSE", "VORKASSE", "PREPAID", "PREPAYMENT" -> "VORAUSKASSE"
            "SEPA_LASTSCHRIFT", "LASTSCHRIFT", "SEPA", "SEPA_DIRECT_DEBIT", "DIRECT_DEBIT", "BANKEINZUG" -> "SEPA_LASTSCHRIFT"
            "KREDITKARTE", "CREDIT_CARD", "MASTERCARD", "VISA", "EC", "GIROCARD", "DEBIT_CARD" -> "KREDITKARTE"
            "PAYPAL" -> "PAYPAL"
            "AMAZON_PAY", "AMAZONPAY" -> "AMAZON_PAY"
            "UEBERWEISUNG", "ÜBERWEISUNG", "BANK_TRANSFER", "TRANSFER" -> "UEBERWEISUNG"
            "BAR", "BARZAHLUNG", "CASH" -> "BAR"
            else -> "SONSTIGE"
        }
    }

    private fun buildMultiDocumentPrompt(): String = """
        Analysiere dieses PDF auf mehrere separate Rechnungen oder Geschaeftsdokumente.
        Antworte nur mit einem JSON-Array. Pro Dokument:
        {"seiten":"1-2","dokumentTyp":"RECHNUNG","dokumentNummer":"...","dokumentDatum":"YYYY-MM-DD","betragBrutto":123.45,"betragNetto":103.74,"bestellnummer":"...","referenzNummer":"...","bereitsGezahlt":true,"zahlungsart":"SEPA_LASTSCHRIFT","confidence":0.95}
        Wenn nur ein Dokument vorhanden ist, gib ein Array mit einem Element und "seiten":"alle" zurueck.
    """.trimIndent()

    private fun dokumentAnalysePrompt(): String = """
        Analysiere dieses deutsche Geschaeftsdokument. Antworte ausschliesslich als JSON.
        Felder: dokumentTyp (ANGEBOT, AUFTRAGSBESTAETIGUNG, LIEFERSCHEIN, RECHNUNG, GUTSCHRIFT, SONSTIG),
        istGeschaeftsdokument, dokumentNummer, dokumentDatum (YYYY-MM-DD), betragNetto, betragBrutto, mwstSatz,
        liefertermin, zahlungsziel, bestellnummer, referenzNummer, bereitsGezahlt, zahlungsart,
        skontoTage, skontoProzent, nettoTage, confidence, lieferantName, lieferantStrasse, lieferantPlz, lieferantOrt.
    """.trimIndent()
    fun findeLieferantByEmailDomain(emailAddress: String?): Optional<Lieferanten> {
        if (emailAddress.isNullOrBlank() || !emailAddress.contains("@")) {
            return Optional.empty()
        }
        val domain = emailAddress.substringAfterLast("@").trim().lowercase()
        if (domain.isBlank()) {
            return Optional.empty()
        }
        return lieferantenRepository.findByEmailDomain(domain).firstOrNull()?.let { Optional.of(it) }
            ?: Optional.empty()
    }
    fun performRelink(dokument: LieferantDokument) {
        val lieferantId = dokument.lieferant?.id ?: return
        val alleDokumente = dokumentRepository.findByLieferantIdOrderByUploadDatumDesc(lieferantId)
        performRelink(dokument, alleDokumente)
    }

    fun performRelink(dokument: LieferantDokument, alleDokumente: List<LieferantDokument>?) {
        val geschaeftsdaten = dokument.geschaeftsdaten ?: return
        if (dokument.lieferant == null) return

        automatischeVerknuepfung(dokument, geschaeftsdaten, alleDokumente)

        val meineDokumentNummer = geschaeftsdaten.dokumentNummer?.trim()?.takeIf { it.isNotBlank() } ?: return
        alleDokumente.orEmpty()
            .asSequence()
            .filter {
                it.id != dokument.id &&
                    it.geschaeftsdaten != null &&
                    it.lieferant?.id == dokument.lieferant?.id
            }
            .forEach { anderes ->
                val fremdReferenz = anderes.geschaeftsdaten?.referenzNummer?.trim()
                if (fremdReferenz != null && fremdReferenz.equals(meineDokumentNummer, ignoreCase = true)) {
                    if (anderes.verknuepfteDokumente.add(dokument)) {
                        dokumentRepository.save(anderes)
                        log.info("[Relink] Dokument {} verweist auf {} (Ref: {})", anderes.id, dokument.id, fremdReferenz)
                    }
                }
            }
    }

    @Transactional
    open fun relinkAlleDokumente(): Int {
        val alleDokumente = dokumentRepository.findAll()
        var verknuepft = 0

        alleDokumente
            .filter { it.geschaeftsdaten != null }
            .forEach { dokument ->
                val vorher = dokument.verknuepfteDokumente.size
                dokument.verknuepfteDokumente.clear()
                performRelink(dokument, alleDokumente)
                dokumentRepository.save(dokument)
                if (dokument.verknuepfteDokumente.size > vorher) {
                    verknuepft++
                }
            }

        log.info("[Relink] Fertig! {} von {} Dokumenten neu verknuepft.", verknuepft, alleDokumente.size)
        return verknuepft
    }

    @Transactional
    open fun relinkDokumenteByLieferant(lieferantId: Long): Int {
        val dokumente = dokumentRepository.findByLieferantIdOrderByUploadDatumDesc(lieferantId)
        var verknuepft = 0

        dokumente
            .filter { it.geschaeftsdaten != null }
            .forEach { dokument ->
                val vorher = dokument.verknuepfteDokumente.size
                dokument.verknuepfteDokumente.clear()
                performRelink(dokument, dokumente)
                dokumentRepository.save(dokument)
                if (dokument.verknuepfteDokumente.size > vorher) {
                    verknuepft++
                }
            }

        log.info("[Relink] Fertig fuer Lieferant {}! {} von {} Dokumenten neu verknuepft.", lieferantId, verknuepft, dokumente.size)
        return verknuepft
    }

    private fun automatischeVerknuepfung(
        dokument: LieferantDokument,
        geschaeftsdaten: LieferantGeschaeftsdokument,
        vorhandeneKandidaten: List<LieferantDokument>? = null,
    ) {
        val lieferantId = dokument.lieferant?.id ?: return
        val referenzNummer = geschaeftsdaten.referenzNummer?.trim()?.takeIf { it.isNotBlank() }
        if (referenzNummer == null && geschaeftsdaten.betragBrutto == null) return

        val vorgaengerTypen = vorgaengerTypenFuer(dokument.typ)
        if (vorgaengerTypen.isEmpty()) return

        val kandidaten = (vorhandeneKandidaten
            ?.filter { it.lieferant?.id == lieferantId && it.typ in vorgaengerTypen }
            ?: dokumentRepository.findByLieferantIdAndTypIn(lieferantId, vorgaengerTypen))
            .filter { it.id != dokument.id && it.geschaeftsdaten != null }

        if (referenzNummer != null) {
            for (kandidat in kandidaten) {
                val kandidatNummer = kandidat.geschaeftsdaten?.dokumentNummer ?: continue
                val exakt = kandidatNummer.trim().equals(referenzNummer, ignoreCase = true)
                val normalisiert = normalizeNummer(kandidatNummer)?.let { it == normalizeNummer(referenzNummer) } == true
                if (exakt || normalisiert) {
                    dokument.verknuepfteDokumente.add(kandidat)
                    log.info("[Verknuepfung] Dokument {} -> {} (Referenz: {})", dokument.id, kandidat.id, referenzNummer)
                    break
                }
            }
        }

        val meinBrutto = geschaeftsdaten.betragBrutto ?: return
        if (dokument.verknuepfteDokumente.isNotEmpty()) return

        val meinDatum = geschaeftsdaten.dokumentDatum
        for (kandidat in kandidaten) {
            val kandidatDaten = kandidat.geschaeftsdaten ?: continue
            val kandidatBrutto = kandidatDaten.betragBrutto ?: continue
            if (meinBrutto.compareTo(kandidatBrutto) != 0) continue

            val kandidatDatum = kandidatDaten.dokumentDatum
            if (meinDatum != null && kandidatDatum != null) {
                val minDate = meinDatum.minusMonths(1)
                val maxDate = meinDatum.plusMonths(1)
                if (kandidatDatum.isBefore(minDate) || kandidatDatum.isAfter(maxDate)) continue
            }

            dokument.verknuepfteDokumente.add(kandidat)
            log.info("[Verknuepfung] Fallback-Match Dokument {} -> {}", dokument.id, kandidat.id)
            break
        }
    }

    private fun vorgaengerTypenFuer(typ: LieferantDokumentTyp?): List<LieferantDokumentTyp> =
        when (typ) {
            LieferantDokumentTyp.RECHNUNG -> listOf(LieferantDokumentTyp.AUFTRAGSBESTAETIGUNG, LieferantDokumentTyp.LIEFERSCHEIN)
            LieferantDokumentTyp.GUTSCHRIFT -> listOf(LieferantDokumentTyp.RECHNUNG)
            LieferantDokumentTyp.LIEFERSCHEIN -> listOf(LieferantDokumentTyp.AUFTRAGSBESTAETIGUNG)
            LieferantDokumentTyp.AUFTRAGSBESTAETIGUNG -> listOf(LieferantDokumentTyp.ANGEBOT)
            else -> emptyList()
        }

    private fun normalizeNummer(nummer: String?): String? {
        val normalized = nummer
            ?.takeIf { it.isNotBlank() }
            ?.replace(Regex("[^a-zA-Z0-9]"), "")
            ?.uppercase()
        return normalized?.takeIf { it.isNotEmpty() }
    }

    private fun ZugferdDaten.hasUsefulData(): Boolean =
        !rechnungsnummer.isNullOrBlank() ||
            rechnungsdatum != null ||
            betrag != null ||
            betragNetto != null ||
            faelligkeitsdatum != null ||
            !bestellnummer.isNullOrBlank() ||
            !referenzNummer.isNullOrBlank()

    private fun toGeschaeftsdokument(daten: ZugferdDaten): LieferantGeschaeftsdokument =
        LieferantGeschaeftsdokument().apply {
            dokumentNummer = daten.rechnungsnummer
            dokumentDatum = daten.rechnungsdatum
            betragNetto = daten.betragNetto
            betragBrutto = daten.betrag
            mwstSatz = daten.mwstSatz
            zahlungsziel = daten.faelligkeitsdatum
            bestellnummer = daten.bestellnummer
            referenzNummer = daten.referenzNummer
            bereitsGezahlt = daten.bereitsGezahlt
            skontoTage = daten.skontoTage
            skontoProzent = daten.skontoProzent
            nettoTage = daten.nettoTage
            aiConfidence = 1.0
            datenquelle = "ZUGFeRD/XML"
            detectedTyp = dokumentTypAusZugferdArt(daten.geschaeftsdokumentart)
        }

    private fun toAnalyzeResponse(daten: LieferantGeschaeftsdokument): LieferantDokumentDto.AnalyzeResponse =
        LieferantDokumentDto.AnalyzeResponse.builder()
            .dokumentTyp(daten.detectedTyp ?: erkenneTypAusNummer(daten.dokumentNummer))
            .dokumentNummer(daten.dokumentNummer)
            .dokumentDatum(daten.dokumentDatum)
            .betragNetto(daten.betragNetto)
            .betragBrutto(daten.betragBrutto)
            .mwstSatz(daten.mwstSatz)
            .liefertermin(daten.liefertermin)
            .zahlungsziel(daten.zahlungsziel)
            .bestellnummer(daten.bestellnummer)
            .referenzNummer(daten.referenzNummer)
            .skontoTage(daten.skontoTage)
            .skontoProzent(daten.skontoProzent)
            .nettoTage(daten.nettoTage)
            .bereitsGezahlt(daten.bereitsGezahlt)
            .zahlungsart(daten.zahlungsart)
            .aiConfidence(daten.aiConfidence)
            .analyseQuelle(daten.datenquelle)
            .build()

    private fun dokumentTypAusZugferdArt(art: String?): LieferantDokumentTyp? =
        when (art?.lowercase()) {
            "rechnung" -> LieferantDokumentTyp.RECHNUNG
            "gutschrift" -> LieferantDokumentTyp.GUTSCHRIFT
            "angebot" -> LieferantDokumentTyp.ANGEBOT
            "auftragsbestätigung", "auftragsbestaetigung" -> LieferantDokumentTyp.AUFTRAGSBESTAETIGUNG
            "lieferschein" -> LieferantDokumentTyp.LIEFERSCHEIN
            else -> null
        }

    private fun erkenneTypAusNummer(nummer: String?): LieferantDokumentTyp? {
        val normalized = nummer?.trim()?.uppercase() ?: return null
        return when {
            normalized.startsWith("RE") || normalized.startsWith("RG") || normalized.startsWith("R-") -> LieferantDokumentTyp.RECHNUNG
            normalized.startsWith("AB") || normalized.startsWith("AUF") -> LieferantDokumentTyp.AUFTRAGSBESTAETIGUNG
            normalized.startsWith("LS") || normalized.startsWith("L-") -> LieferantDokumentTyp.LIEFERSCHEIN
            normalized.startsWith("AN") || normalized.startsWith("AG") -> LieferantDokumentTyp.ANGEBOT
            normalized.startsWith("GS") || normalized.startsWith("GU") -> LieferantDokumentTyp.GUTSCHRIFT
            else -> null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GeminiDokumentAnalyseService::class.java)
        private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
        private val apiLock = ReentrantLock(true)
        private const val API_CALL_DELAY_MS = 500L
        @Volatile private var lastApiCallTime = 0L
    }

}

@Suppress("UNCHECKED_CAST")
fun <T> LieferantDokumentDto.AnalyzeResponse.value(key: String): T? =
    when (key) {
        "dokumentTyp" -> dokumentTyp
        "dokumentNummer" -> dokumentNummer
        "dokumentDatum" -> dokumentDatum
        "betragNetto" -> betragNetto
        "betragBrutto" -> betragBrutto
        "mwstSatz" -> mwstSatz
        "zahlungsziel" -> zahlungsziel
        "bestellnummer" -> bestellnummer
        "referenzNummer" -> referenzNummer
        "skontoTage" -> skontoTage
        "skontoProzent" -> skontoProzent
        "nettoTage" -> nettoTage
        "bereitsGezahlt" -> bereitsGezahlt
        "zahlungsart" -> zahlungsart
        "aiConfidence" -> aiConfidence
        "lieferantName" -> lieferantName
        else -> null
    } as T?
