package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.LieferantDokument
import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp
import org.example.kalkulationsprogramm.domain.LieferantGeschaeftsdokument
import org.example.kalkulationsprogramm.domain.Lieferanten
import org.example.kalkulationsprogramm.dto.LieferantDokumentDto
import org.example.kalkulationsprogramm.dto.Zugferd.ZugferdDaten
import org.example.kalkulationsprogramm.repository.LieferantDokumentRepository
import org.example.kalkulationsprogramm.repository.LieferantenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

@Service
open class GeminiDokumentAnalyseService(
    private val lieferantenRepository: LieferantenRepository,
    private val dokumentRepository: LieferantDokumentRepository,
    private val zugferdExtractorService: ZugferdExtractorService,
) {
    fun analysiereDokument(dokument: LieferantDokument): LieferantGeschaeftsdokument = LieferantGeschaeftsdokument()
    fun reanalysiereDokumentById(dokumentId: Long): LieferantGeschaeftsdokument = LieferantGeschaeftsdokument()
    fun analyzeAsync(dokumentId: Long) {}
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

    fun analyzeFile(file: Path, originalDateiname: String?, useProModel: Boolean): LieferantDokumentDto.AnalyzeResponse =
        analyzeAndReturnData(file, originalDateiname)?.let(::toAnalyzeResponse)
            ?: LieferantDokumentDto.AnalyzeResponse()

    fun analyzeFileForMultipleInvoices(file: MultipartFile): MutableList<LieferantDokumentDto.MultiInvoiceAnalyzeResponse> = mutableListOf()
    fun analyzeFileForMultipleInvoices(file: Path, originalDateiname: String?): MutableList<LieferantDokumentDto.MultiInvoiceAnalyzeResponse> = mutableListOf()
    fun rufGeminiApiMitPrompt(bytes: ByteArray, mimeType: String?, customPrompt: String?): String =
        rufGeminiApiMitPrompt(bytes, mimeType, customPrompt, false)
    fun rufGeminiApiMitPrompt(bytes: ByteArray, mimeType: String?, customPrompt: String?, useProModel: Boolean): String = ""
    fun analysiereDokument(dokument: LieferantDokument, explicitPath: Path): LieferantGeschaeftsdokument = LieferantGeschaeftsdokument()
    fun analyzeAndReturnData(dateiPfad: Path, originalDateiname: String?): LieferantGeschaeftsdokument? {
        val name = originalDateiname ?: dateiPfad.fileName?.toString()
        if (!name.orEmpty().lowercase().endsWith(".pdf")) {
            log.info("Analyse ohne KI uebersprungen fuer Nicht-PDF: {}", name)
            return null
        }

        val zugferdDaten = runCatching {
            zugferdExtractorService.extract(dateiPfad.toString(), name)
        }.onFailure {
            log.info("ZUGFeRD-Analyse fehlgeschlagen fuer {}: {}", name, it.message)
        }.getOrNull() ?: return null

        if (!zugferdDaten.hasUsefulData()) {
            return null
        }

        return toGeschaeftsdokument(zugferdDaten)
    }
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
