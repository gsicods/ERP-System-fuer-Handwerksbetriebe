package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.*
import org.example.kalkulationsprogramm.dto.Projekt.UmsatzStatistikDto
import org.example.kalkulationsprogramm.repository.AnfrageDokumentRepository
import org.example.kalkulationsprogramm.repository.AnfrageRepository
import org.example.kalkulationsprogramm.repository.ProjektDokumentRepository
import org.example.kalkulationsprogramm.repository.ProjektRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.math.BigDecimal
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.Month
import java.util.Locale
import java.util.UUID

@Service
open class DateiSpeicherService(
    @Value("\${file.upload-dir}") uploadDir: String,
    @Value("\${file.offer-upload-dir:\${file.upload-dir}}") offerUploadDir: String,
    @Value("\${file.image-upload-dir}") imageUploadDir: String,
    @Value("\${file.cutaway_images-dir:\${file.image-upload-dir}}") cutawayImagesDir: String,
    @Value("\${hicad.local-path:\${file.upload-dir}/CADdrawings}") hicadLocalPath: String,
    @Value("\${hicad.network-url:}") private val hicadNetworkUrl: String,
    @Value("\${hicad.network-drive-letter:}") private val networkDriveLetter: String,
    private val projektDokumentRepository: ProjektDokumentRepository,
    private val anfrageDokumentRepository: AnfrageDokumentRepository,
    private val projektRepository: ProjektRepository,
    private val anfrageRepository: AnfrageRepository,
) {
    private val dokumentenSpeicherplatz: Path = Path.of(uploadDir).toAbsolutePath().normalize()
    private val anfragenSpeicherplatz: Path = Path.of(offerUploadDir).toAbsolutePath().normalize()
    private val bilderSpeicherplatz: Path = Path.of(imageUploadDir).toAbsolutePath().normalize()
    private val schnittbilderSpeicherplatz: Path = Path.of(cutawayImagesDir).toAbsolutePath().normalize()
    private val hicadSpeicherplatz: Path = Path.of(hicadLocalPath).toAbsolutePath().normalize()

    init {
        listOf(dokumentenSpeicherplatz, anfragenSpeicherplatz, bilderSpeicherplatz, schnittbilderSpeicherplatz, hicadSpeicherplatz)
            .forEach { Files.createDirectories(it) }
    }

    fun speichereDatei(datei: MultipartFile, projektID: Long): ProjektDokument = speichereDatei(datei, projektID, DokumentGruppe.DIVERSE_DOKUMENTE)

    @Transactional
    open fun speichereDatei(datei: MultipartFile, projektID: Long, gruppe: DokumentGruppe?): ProjektDokument =
        speichereDatei(datei, projektID, gruppe, null)

    @Transactional
    open fun speichereDatei(
        datei: MultipartFile,
        projektID: Long,
        gruppe: DokumentGruppe?,
        geschaeftsdokumentart: String?,
    ): ProjektDokument {
        val projekt = projektRepository.findById(projektID).orElseThrow { RuntimeException("Projekt nicht gefunden!") }
        val originalDateiname = cleanOriginalName(datei.originalFilename)
        val gespeicherterDateiname = generiereEinzigartigenDateinamen(originalDateiname)
        val zielPfad = resolveAndValidate(storageFor(originalDateiname, dokumentenSpeicherplatz), gespeicherterDateiname)
        copyMultipart(datei, zielPfad)

        val dokument = if (!geschaeftsdokumentart.isNullOrBlank()) {
            ProjektGeschaeftsdokument().apply {
                dokumentid = originalDateiname.substringBeforeLast('.')
                this.geschaeftsdokumentart = geschaeftsdokumentart
                bezahlt = false
            }
        } else {
            ProjektDokument()
        }
        dokument.projekt = projekt
        dokument.originalDateiname = originalDateiname
        dokument.gespeicherterDateiname = gespeicherterDateiname
        dokument.dateityp = datei.contentType
        dokument.dateigroesse = datei.size
        dokument.uploadDatum = LocalDate.now()
        dokument.dokumentGruppe = gruppe ?: DokumentGruppe.DIVERSE_DOKUMENTE
        return projektDokumentRepository.save(dokument)
    }

    @Transactional
    open fun speichereAnfragesDatei(datei: MultipartFile, anfrageID: Long, gruppe: DokumentGruppe?): AnfrageDokument {
        val anfrage = anfrageRepository.findById(anfrageID).orElseThrow { RuntimeException("Anfrage nicht gefunden!") }
        val originalDateiname = cleanOriginalName(datei.originalFilename)
        val gespeicherterDateiname = generiereEinzigartigenDateinamen(originalDateiname)
        val zielPfad = resolveAndValidate(storageFor(originalDateiname, anfragenSpeicherplatz), gespeicherterDateiname)
        copyMultipart(datei, zielPfad)
        val dokument = if (isDrawing(originalDateiname)) {
            AnfrageGeschaeftsdokument().apply {
                dokumentid = originalDateiname.substringBeforeLast('.')
                geschaeftsdokumentart = "Zeichnung"
                bruttoBetrag = anfrage.betrag
            }
        } else {
            AnfrageDokument()
        }
        dokument.anfrage = anfrage
        dokument.originalDateiname = originalDateiname
        dokument.gespeicherterDateiname = gespeicherterDateiname
        dokument.dateityp = datei.contentType
        dokument.dateigroesse = datei.size
        dokument.uploadDatum = LocalDate.now()
        dokument.dokumentGruppe = gruppe ?: DokumentGruppe.DIVERSE_DOKUMENTE
        return anfrageDokumentRepository.save(dokument)
    }

    @Transactional
    open fun speichereZugferdDatei(zugferdPfad: Path, originalDateiname: String?, projektID: Long, art: String?): ProjektGeschaeftsdokument {
        val projekt = projektRepository.findById(projektID).orElseThrow { RuntimeException("Projekt nicht gefunden!") }
        val original = cleanOriginalName(originalDateiname)
        val stored = generiereEinzigartigenDateinamen(original)
        val ziel = resolveAndValidate(dokumentenSpeicherplatz, stored)
        Files.copy(zugferdPfad, ziel, StandardCopyOption.REPLACE_EXISTING)
        val dokument = ProjektGeschaeftsdokument().apply {
            this.projekt = projekt
            this.originalDateiname = original
            gespeicherterDateiname = stored
            dateityp = "application/pdf"
            dateigroesse = safeSize(ziel)
            uploadDatum = LocalDate.now()
            dokumentGruppe = DokumentGruppe.GESCHAEFTSDOKUMENTE
            dokumentid = original.substringBeforeLast('.')
            geschaeftsdokumentart = art ?: "Rechnung"
            bezahlt = false
        }
        return projektDokumentRepository.save(dokument) as ProjektGeschaeftsdokument
    }

    @Transactional
    open fun speichereAnfragesZugferdDatei(zugferdPfad: Path, originalDateiname: String?, anfrageID: Long, art: String?): AnfrageGeschaeftsdokument =
        speichereAnfragesZugferdDatei(zugferdPfad, originalDateiname, anfrageID, null).apply {
            if (!art.isNullOrBlank()) geschaeftsdokumentart = art
        }

    @Transactional
    open fun speichereAnfragesZugferdDatei(zugferdPfad: Path, originalDateiname: String?, anfrageID: Long, daten: Any?): AnfrageGeschaeftsdokument {
        val anfrage = anfrageRepository.findById(anfrageID).orElseThrow { RuntimeException("Anfrage nicht gefunden!") }
        val original = cleanOriginalName(originalDateiname)
        val stored = generiereEinzigartigenDateinamen(original)
        val ziel = resolveAndValidate(anfragenSpeicherplatz, stored)
        Files.copy(zugferdPfad, ziel, StandardCopyOption.REPLACE_EXISTING)
        val dokument = AnfrageGeschaeftsdokument().apply {
            this.anfrage = anfrage
            this.originalDateiname = original
            gespeicherterDateiname = stored
            dateityp = "application/pdf"
            dateigroesse = safeSize(ziel)
            uploadDatum = LocalDate.now()
            dokumentGruppe = DokumentGruppe.GESCHAEFTSDOKUMENTE
            dokumentid = original.substringBeforeLast('.')
            geschaeftsdokumentart = "Angebot"
            bruttoBetrag = anfrage.betrag
        }
        return anfrageDokumentRepository.save(dokument) as AnfrageGeschaeftsdokument
    }

    @Transactional
    open fun speichereErzeugteDatei(inhalt: ByteArray, dateiname: String?, projektID: Long, gruppe: DokumentGruppe?): ProjektDokument {
        val projekt = projektRepository.findById(projektID).orElseThrow { RuntimeException("Projekt nicht gefunden!") }
        val original = cleanOriginalName(dateiname)
        val stored = generiereEinzigartigenDateinamen(original)
        val ziel = resolveAndValidate(dokumentenSpeicherplatz, stored)
        Files.write(ziel, inhalt)
        val dokument = ProjektDokument().apply {
            this.projekt = projekt
            originalDateiname = original
            gespeicherterDateiname = stored
            dateityp = "application/pdf"
            dateigroesse = safeSize(ziel)
            uploadDatum = LocalDate.now()
            dokumentGruppe = gruppe ?: DokumentGruppe.PLANUNGSDOKUMENTE
        }
        return projektDokumentRepository.save(dokument)
    }

    @Transactional
    open fun verschiebeAnfragesDatei(anfrageDokument: AnfrageDokument, projekt: Projekt) {
        val dokument = if (anfrageDokument is AnfrageGeschaeftsdokument) {
            ProjektGeschaeftsdokument().apply {
                dokumentid = anfrageDokument.dokumentid
                geschaeftsdokumentart = anfrageDokument.geschaeftsdokumentart
                bruttoBetrag = anfrageDokument.bruttoBetrag
                bezahlt = false
            }
        } else {
            ProjektDokument()
        }
        dokument.projekt = projekt
        dokument.originalDateiname = anfrageDokument.originalDateiname
        dokument.gespeicherterDateiname = anfrageDokument.gespeicherterDateiname
        dokument.dateityp = anfrageDokument.dateityp
        dokument.dateigroesse = anfrageDokument.dateigroesse
        dokument.uploadDatum = anfrageDokument.uploadDatum
        dokument.emailVersandDatum = anfrageDokument.emailVersandDatum
        dokument.dokumentGruppe = anfrageDokument.dokumentGruppe
        projektDokumentRepository.save(dokument)
    }

    fun aktualisiereProjektFinanzstatus(projektID: Long) {}
    fun holeDokumenteZuProjekt(projektID: Long): List<ProjektDokument> =
        projektDokumentRepository.findByProjektId(projektID)

    fun holeDokumenteZuAnfrage(anfrageID: Long): List<AnfrageDokument> =
        anfrageDokumentRepository.findByAnfrageId(anfrageID)

    fun holeOffeneGeschaeftsdokumente(): List<ProjektGeschaeftsdokument> =
        projektDokumentRepository.findOffeneGeschaeftsdokumente()

    fun holeRechnungenZuProjekt(projektId: Long): List<ProjektGeschaeftsdokument> =
        projektDokumentRepository.findRechnungenByProjektId(projektId)

    fun holeGeschaeftsdokumenteNachJahrUndFilter(jahr: Int, monat: Int?, bezahlt: Boolean?): List<ProjektGeschaeftsdokument> {
        val start = if (monat == null) {
            LocalDate.of(jahr, Month.JANUARY, 1)
        } else {
            LocalDate.of(jahr, monat.coerceIn(1, 12), 1)
        }
        val end = if (monat == null) start.plusYears(1).minusDays(1) else start.plusMonths(1).minusDays(1)
        return projektDokumentRepository.findGeschaeftsdokumenteByRechnungsdatumBetween(start, end)
            .filter { bezahlt == null || it.bezahlt == bezahlt }
    }

    fun berechneProjektArbeitskosten(projekt: Projekt?): Double =
        projekt?.zeitbuchungen.orEmpty().fold(BigDecimal.ZERO) { sum, zeit ->
            val stunden = zeit.anzahlInStunden ?: BigDecimal.ZERO
            val satz = zeit.arbeitsgangStundensatz?.satz ?: BigDecimal.ZERO
            sum.add(stunden.multiply(satz))
        }.toDouble()

    fun berechneProjektMaterialkosten(projekt: Projekt?): Double =
        projekt?.materialkosten.orEmpty().fold(BigDecimal.ZERO) { sum, material -> sum.add(material.betrag) }.toDouble()

    fun berechneProjektMaterialkosten(projekt: Projekt?, monat: Int?): Double =
        projekt?.materialkosten.orEmpty()
            .asSequence()
            .filter { monat == null || it.monat == monat }
            .fold(BigDecimal.ZERO) { sum, material -> sum.add(material.betrag) }
            .toDouble()

    fun berechneProjektKosten(projekt: Projekt?): Double =
        berechneProjektArbeitskosten(projekt) + berechneProjektMaterialkosten(projekt)
    fun holeUmsatzStatistiken(jahr: Int, monat: Int?): UmsatzStatistikDto = UmsatzStatistikDto()
    fun holeDokument(dokumentID: Long): ProjektDokument? =
        projektDokumentRepository.findById(dokumentID).orElse(null)

    @Transactional
    open fun setzeGeschaeftsdokumentBezahlt(dokumentID: Long, bezahlt: Boolean) {
        val dokument = projektDokumentRepository.findById(dokumentID).orElseThrow { RuntimeException("Projektdokument konnte nicht gefunden werden!") }
        if (dokument !is ProjektGeschaeftsdokument) throw RuntimeException("Dokument ist kein Geschaeftsdokument.")
        dokument.bezahlt = bezahlt
        projektDokumentRepository.save(dokument)
        dokument.projekt?.let { pruefeProjektAbschluss(it) }
    }

    @Transactional
    open fun loescheDatei(dokumentID: Long) {
        val dokument = projektDokumentRepository.findById(dokumentID).orElseThrow { RuntimeException("Projektdokument konnte nicht gefunden werden!") }
        deleteFromKnownStorages(dokument.gespeicherterDateiname)
        projektDokumentRepository.delete(dokument)
    }

    @Transactional
    open fun loescheAnfrageDatei(dokumentID: Long?) {
        val id = dokumentID ?: return
        val dokument = anfrageDokumentRepository.findById(id).orElseThrow { RuntimeException("Anfragedokument konnte nicht gefunden werden!") }
        deleteFromKnownStorages(dokument.gespeicherterDateiname)
        anfrageDokumentRepository.delete(dokument)
    }

    open fun speichereBild(datei: MultipartFile): String {
        val contentType = datei.contentType
        if (contentType == null || contentType !in ERLAUBTE_BILD_TYPEN) {
            throw RuntimeException("Ungueltiger Dateityp! Nur JPEG, PNG, GIF und WebP sind erlaubt.")
        }
        val original = cleanOriginalName(datei.originalFilename)
        val stored = generiereEinzigartigenDateinamen(original)
        copyMultipart(datei, resolveAndValidate(bilderSpeicherplatz, stored))
        return "/api/images/$stored"
    }

    fun kopiereBildZuDokumenten(quellDateiname: String?): String = quellDateiname.orEmpty()

    @Transactional
    open fun loescheBild(bildUrl: String?) {
        if (bildUrl.isNullOrBlank()) return
        Files.deleteIfExists(resolveAndValidate(bilderSpeicherplatz, Path.of(bildUrl).fileName.toString()))
    }

    open fun ladeBildAlsResource(dateiname: String?): Resource =
        loadFirstReadable(dateiname, listOf(bilderSpeicherplatz, schnittbilderSpeicherplatz))

    open fun loescheDokumentPdfByDateiname(dateiname: String?) {
        if (dateiname.isNullOrBlank()) return
        runCatching { Files.deleteIfExists(resolveAndValidate(dokumentenSpeicherplatz, dateiname)) }
            .onFailure { log.warn("Freigabe-PDF konnte nicht geloescht werden: {}", dateiname, it) }
    }

    open fun ladeDokumentAlsResource(dateiname: String?): Resource =
        loadFirstReadable(dateiname, listOf(dokumentenSpeicherplatz, anfragenSpeicherplatz, hicadSpeicherplatz))

    open fun liegtInHicadSpeicher(dateiname: String?): Boolean =
        !dateiname.isNullOrBlank() && Files.exists(resolveAndValidate(hicadSpeicherplatz, dateiname))

    open fun holeNetzwerkPfad(relativerPfad: String?): String {
        val cleaned = safeRelativePath(relativerPfad).replace("/", "\\")
        val base = hicadNetworkUrl.trimEnd('\\')
        if (base.isBlank()) throw IllegalStateException("hicad.network-url nije konfiguriran")
        return "$base\\$cleaned"
    }

    open fun holeWindowsLaufwerkPfad(relativerPfad: String?): String {
        val letter = networkDriveLetter.trim().let { if (it.endsWith(":")) it else "$it:" }
        if (letter == ":") return ""
        return "$letter\\${safeRelativePath(relativerPfad).replace("/", "\\")}"
    }
    fun ladeDokumentMetadaten(dateiname: String?): Dokument {
        val normalized = dateiname?.trim().orEmpty()
        if (normalized.isNotEmpty()) {
            projektDokumentRepository.findByGespeicherterDateinameIgnoreCase(normalized).orElse(null)?.let { return it }
            anfrageDokumentRepository.findByGespeicherterDateinameIgnoreCase(normalized).orElse(null)?.let { return it }
        }
        return object : Dokument {
                override val id: Long? = null
                override val originalDateiname: String? = dateiname
                override val gespeicherterDateiname: String? = dateiname
                override val dateityp: String? = null
                override val dateigroesse: Long? = null
                override val uploadDatum: LocalDate? = null
                override val emailVersandDatum: LocalDate? = null
                override val dokumentGruppe: DokumentGruppe? = null
            }
    }

    private fun pruefeProjektAbschluss(projekt: Projekt) {
        val id = projekt.id ?: return
        val keineOffenenPosten = !projektDokumentRepository.existsOffenePostenByProjektId(id)
        if (keineOffenenPosten) {
            projekt.bezahlt = true
            projekt.abgeschlossen = true
            projektRepository.save(projekt)
        } else {
            projekt.bezahlt = false
            projekt.abgeschlossen = false
            projektRepository.save(projekt)
        }
    }

    private fun storageFor(originalDateiname: String, normalStorage: Path): Path =
        if (isHicadDatei(originalDateiname)) hicadSpeicherplatz else normalStorage

    private fun copyMultipart(datei: MultipartFile, zielPfad: Path) {
        try {
            datei.inputStream.use { Files.copy(it, zielPfad, StandardCopyOption.REPLACE_EXISTING) }
        } catch (ex: IOException) {
            throw RuntimeException("Datei konnte nicht gespeichert werden.", ex)
        }
    }

    private fun loadFirstReadable(dateiname: String?, bases: List<Path>): Resource {
        val name = cleanStoredName(dateiname)
        for (base in bases) {
            val path = resolveAndValidate(base, name)
            if (Files.exists(path)) {
                val resource = toResource(path)
                if (resource.exists() && resource.isReadable) return resource
            }
        }
        for (base in bases) {
            Files.list(base).use { stream ->
                stream.filter { it.fileName.toString().equals(name, ignoreCase = true) }.findFirst().orElse(null)?.let {
                    val resource = toResource(it)
                    if (resource.exists() && resource.isReadable) return resource
                }
            }
        }
        throw RuntimeException("Datei nicht gefunden oder nicht lesbar: $name")
    }

    private fun toResource(path: Path): Resource =
        try {
            UrlResource(path.toUri())
        } catch (ex: MalformedURLException) {
            throw RuntimeException("Fehler beim Lesen der Datei: $path", ex)
        }

    private fun deleteFromKnownStorages(dateiname: String?) {
        if (dateiname.isNullOrBlank()) return
        listOf(dokumentenSpeicherplatz, anfragenSpeicherplatz, hicadSpeicherplatz, bilderSpeicherplatz, schnittbilderSpeicherplatz)
            .forEach { runCatching { Files.deleteIfExists(resolveAndValidate(it, dateiname)) } }
    }

    private fun safeSize(path: Path): Long = runCatching { Files.size(path) }.getOrDefault(0L)

    private fun cleanOriginalName(dateiname: String?): String {
        val cleaned = StringUtils.cleanPath(dateiname ?: "datei.bin")
        return Path.of(cleaned).fileName.toString().ifBlank { "datei.bin" }
    }

    private fun cleanStoredName(dateiname: String?): String =
        Path.of(dateiname ?: "").fileName.toString().takeIf { it.isNotBlank() }
            ?: throw RuntimeException("Dateiname fehlt")

    private fun generiereEinzigartigenDateinamen(dateiname: String): String {
        val original = cleanOriginalName(dateiname)
        val dot = original.lastIndexOf('.')
        val extension = if (dot >= 0) original.substring(dot) else ""
        val base = if (dot >= 0) original.substring(0, dot) else original
        val safeBase = base.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "datei" }
        return "${UUID.randomUUID()}_$safeBase$extension"
    }

    private fun resolveAndValidate(baseDir: Path, filename: String): Path {
        val safeName = Path.of(filename).fileName.toString()
        val resolved = baseDir.resolve(safeName).normalize()
        if (!resolved.startsWith(baseDir)) throw SecurityException("Ungueltiger Dateipfad")
        return resolved
    }

    private fun safeRelativePath(path: String?): String {
        val cleaned = path?.trim()?.replace("\\", "/").orEmpty()
        if (cleaned.isBlank() || cleaned.contains("..")) throw SecurityException("Pfad ausserhalb des freigegebenen Verzeichnisses")
        return cleaned.trimStart('/')
    }

    private fun isDrawing(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower.contains("zeichnung") || lower.contains("entwurf")
    }

    private fun isHicadDatei(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return HICAD_EXTENSIONS.any { lower.endsWith(it) }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(DateiSpeicherService::class.java)
        private val ERLAUBTE_BILD_TYPEN = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        private val HICAD_EXTENSIONS = setOf(".sza", ".tcd", ".xls", ".xlsx", ".xlsm", ".csv", ".ods", ".xlsb")
    }
}
