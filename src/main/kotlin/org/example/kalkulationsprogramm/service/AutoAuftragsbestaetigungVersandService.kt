package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.email.EmailService
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import org.example.kalkulationsprogramm.domain.DokumentFreigabe
import org.example.kalkulationsprogramm.domain.Kunde
import org.example.kalkulationsprogramm.domain.Textbaustein
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository
import org.example.kalkulationsprogramm.repository.DokumentFreigabeRepository
import org.example.kalkulationsprogramm.service.RechnungPdfService.ContentBlockDto
import org.example.kalkulationsprogramm.service.RechnungPdfService.FormBlockDto
import org.example.kalkulationsprogramm.service.RechnungPdfService.KopfdatenDto
import org.example.kalkulationsprogramm.service.RechnungPdfService.RechnungDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.math.BigDecimal
import java.math.RoundingMode
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Optional

@Service
open class AutoAuftragsbestaetigungVersandService(
    private val rechnungPdfService: RechnungPdfService,
    private val systemSettingsService: SystemSettingsService,
    private val emailTextTemplateService: EmailTextTemplateService,
    private val ausgangsGeschaeftsDokumentRepository: AusgangsGeschaeftsDokumentRepository,
    private val formularTemplateService: FormularTemplateService,
    private val formularTextbausteinDefaultService: FormularTextbausteinDefaultService,
    private val emailSignatureService: EmailSignatureService,
    private val projektEmailArchivService: ProjektEmailArchivService,
    private val dokumentFreigabeRepository: DokumentFreigabeRepository,
) {
    @Transactional
    open fun versendeNachAnnahme(abId: Long?, empfaenger: String?, freigabeUuid: String?): Boolean {
        val id = abId ?: return false
        val ab = ausgangsGeschaeftsDokumentRepository.findById(id).orElse(null) ?: run {
            log.warn("Auto-AB-Versand uebersprungen: Dokument {} nicht mehr vorhanden", id)
            return false
        }
        val freigabe = freigabeUuid?.let { dokumentFreigabeRepository.findByUuid(it).orElse(null) }
        return versende(ab, empfaenger, freigabe)
    }

    open fun versende(ab: AusgangsGeschaeftsDokument?, empfaenger: String?): Boolean =
        versende(ab, empfaenger, null)

    open fun versende(
        ab: AusgangsGeschaeftsDokument?,
        empfaenger: String?,
        freigabe: DokumentFreigabe?,
    ): Boolean {
        if (ab == null || ab.typ != AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG) return false
        if (empfaenger.isNullOrBlank()) {
            log.warn("Auto-AB-Versand uebersprungen: kein Empfaenger fuer AB {}", ab.dokumentNummer)
            return false
        }

        var tempPdf: Path? = null
        return try {
            val pdfBytes = generierePdfFuerAb(ab)
            val filename = "Auftragsbestaetigung_${sanitizeForFilename(ab.dokumentNummer)}.pdf"
            tempPdf = Files.createTempFile("auto-ab-", ".pdf")
            Files.write(tempPdf, pdfBytes)

            var content = baueEmailInhalt(ab)
            if (freigabe != null) {
                content = mitAnnahmeBeleg(content, freigabe)
            }

            if (!systemSettingsService.isSmtpConfigured()) {
                log.warn("Auto-AB-Versand uebersprungen: SMTP ist nicht konfiguriert")
                return false
            }

            val absender = ermittleAbsenderAdresse()
            val htmlMitSignatur = emailSignatureService.appendSystemSignatureIfConfigured(content.htmlBody())
            val messageId = EmailService(
                systemSettingsService.smtpHost,
                systemSettingsService.smtpPort,
                systemSettingsService.smtpUsername,
                systemSettingsService.smtpPassword,
            ).sendEmailAndReturnMessageId(
                empfaenger,
                null,
                absender,
                content.subject(),
                htmlMitSignatur,
                tempPdf.toString(),
                filename,
            )

            archiviereAlsProjektEmail(ab, empfaenger, absender, content.subject(), htmlMitSignatur, messageId, tempPdf, filename)
            markiereAlsVersendet(ab)
            log.info("Auto-AB {} per Mail versendet", ab.dokumentNummer)
            true
        } catch (ex: Exception) {
            log.error("Auto-AB-Versand fuer {} fehlgeschlagen: {}", ab.dokumentNummer, ex.message, ex)
            false
        } finally {
            tempPdf?.let { runCatching { Files.deleteIfExists(it) } }
        }
    }

    internal var archivRetryPauseMillis: Long = 2_000L

    internal open fun archiviereAlsProjektEmail(
        ab: AusgangsGeschaeftsDokument,
        empfaenger: String?,
        absender: String?,
        subject: String?,
        htmlBody: String?,
        messageId: String?,
        pdf: Path,
        dateiname: String?,
    ) {
        val projekt = ab.projekt ?: run {
            log.warn("Auto-AB {} versendet, aber ohne Projekt - Mail kann nicht im E-Mail-Center archiviert werden", ab.dokumentNummer)
            return
        }
        for (versuch in 1..MAX_ARCHIV_VERSUCHE) {
            try {
                projektEmailArchivService.archiviereVersandteEmail(projekt, empfaenger, absender, subject, htmlBody, messageId, pdf, dateiname)
                return
            } catch (ex: Exception) {
                if (versuch == MAX_ARCHIV_VERSUCHE) {
                    log.error(
                        "Auto-AB {} versendet, aber Archivierung im E-Mail-Center nach {} Versuchen fehlgeschlagen: {}",
                        ab.dokumentNummer,
                        versuch,
                        ex.message,
                        ex,
                    )
                    return
                }
                val pause = archivRetryPauseMillis * versuch
                log.warn(
                    "Auto-AB {} Archivierung fehlgeschlagen (Versuch {}/{}): {} - neuer Versuch in {}ms",
                    ab.dokumentNummer,
                    versuch,
                    MAX_ARCHIV_VERSUCHE,
                    ex.message,
                    pause,
                )
                try {
                    Thread.sleep(pause)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    @Transactional
    protected open fun markiereAlsVersendet(ab: AusgangsGeschaeftsDokument) {
        val id = ab.id ?: return
        val frisch = ausgangsGeschaeftsDokumentRepository.findById(id).orElse(null) ?: return
        frisch.versandDatum = LocalDate.now()
        ausgangsGeschaeftsDokumentRepository.save(frisch)
    }

    private fun generierePdfFuerAb(ab: AusgangsGeschaeftsDokument): ByteArray {
        val kopfdaten = buildKopfdaten(ab)
        val templateName = ladeTemplateName(ab).orElse(null)
        val contentBlocks = baueContentBlocks(ab, templateName)
        val vorlage = ladeVorlagenDaten(templateName)
        val layout = if (vorlage.formBlocks.isEmpty()) {
            RechnungPdfService.getDefaultLayout()
        } else {
            RechnungPdfService.createLayoutFromFormBlocks(vorlage.formBlocks, 595f, 842f)
        }
        return rechnungPdfService.generatePdfBytes(
            RechnungDto(
                layout = layout,
                kopfdaten = kopfdaten,
                contentBlocks = contentBlocks,
                formBlocks = vorlage.formBlocks,
                schlusstext = null,
                backgroundImagePage1 = vorlage.backgroundImagePage1,
                backgroundImagePage2 = vorlage.backgroundImagePage2,
                globalRabattProzent = null,
                abrechnungsverlauf = null,
                betragNetto = ab.betragNetto,
                abschlagInfo = null,
            ),
        )
    }

    private fun baueContentBlocks(ab: AusgangsGeschaeftsDokument, templateName: String?): List<ContentBlockDto> {
        val kern = parsePositionenJsonZuContentBlocks(ab.positionenJson)
        if (templateName.isNullOrBlank()) return kern

        val defaults = try {
            formularTextbausteinDefaultService.loadForDokumenttyp(templateName, "Auftragsbestätigung")
        } catch (ex: Exception) {
            log.warn("Standard-Textbausteine fuer AB konnten nicht geladen werden: {}", ex.message)
            return kern
        }

        val ctx = bauePlatzhalterKontext(ab)
        return buildList {
            defaults.vortexte.forEach { add(textbausteinAlsBlock(it, ctx)) }
            kern.forEach { add(loeseBlockAuf(it, ctx)) }
            defaults.nachtexte.forEach { add(textbausteinAlsBlock(it, ctx)) }
        }
    }

    private fun buildKopfdaten(ab: AusgangsGeschaeftsDokument): KopfdatenDto {
        val kunde = effektiverKunde(ab)
        val vorgaenger = ab.vorgaenger
        return KopfdatenDto(
            rechnungsnummer = ab.dokumentNummer,
            rechnungsDatum = ab.datum,
            leistungsDatum = ab.datum,
            kundenName = kunde?.name,
            kundenAdresse = kunde?.let(::baueAdresseAusKunde),
            betreff = ab.betreff,
            kundennummer = kunde?.kundennummer,
            dokumentTyp = "Auftragsbestätigung",
            bezugsdokument = vorgaenger?.dokumentNummer,
            projektnummer = ermittleProjektnummer(ab),
            bauvorhaben = ermittleBauvorhaben(ab),
            bezugsdokumentTyp = vorgaenger?.typ?.let(::typLabel),
            bezugsdokumentDatum = vorgaenger?.datum?.format(DATUM_FORMAT),
            zahlungszielTage = ab.zahlungszielTage,
        )
    }

    internal open fun ladeTemplateName(ab: AusgangsGeschaeftsDokument?): Optional<String> {
        val direkt = ladeTemplateNameFuer("Auftragsbestätigung")
        if (direkt.isPresent) return direkt
        val vorgaengerTyp = ab?.vorgaenger?.typ ?: return Optional.empty()
        return ladeTemplateNameFuer(typLabel(vorgaengerTyp))
    }

    private fun ladeTemplateNameFuer(dokumenttypLabel: String): Optional<String> =
        try {
            formularTemplateService.getPreferredTemplateForDokumenttyp(dokumenttypLabel, null)
        } catch (ex: Exception) {
            log.warn("Vorlagenzuordnung fuer '{}' konnte nicht ermittelt werden: {}", dokumenttypLabel, ex.message)
            Optional.empty()
        }

    private fun ladeVorlagenDaten(templateName: String?): VorlagenDaten {
        if (templateName.isNullOrBlank()) return VorlagenDaten.leer()
        return try {
            parseVorlagenHtml(formularTemplateService.loadNamedTemplate(templateName).html())
        } catch (ex: Exception) {
            log.warn("Vorlage '{}' konnte nicht geladen werden, fallback auf Standard-Briefkopf: {}", templateName, ex.message)
            VorlagenDaten.leer()
        }
    }

    private fun baueEmailInhalt(ab: AusgangsGeschaeftsDokument): EmailService.EmailContent {
        val ctx = bauePlatzhalterKontext(ab)
        val templateContent = runCatching { emailTextTemplateService.render("Auftragsbestätigung", ctx) }.getOrNull()
        if (templateContent != null && !templateContent.subject().isNullOrBlank()) {
            return templateContent
        }
        val nummer = ab.dokumentNummer ?: ""
        val subject = if (nummer.isBlank()) "Ihre Auftragsbestätigung" else "Ihre Auftragsbestätigung $nummer"
        val body = "<p>${ctx["ANREDE"] ?: "Sehr geehrte Damen und Herren"},</p>" +
            "<p>anbei erhalten Sie Ihre Auftragsbestätigung.</p>" +
            "<p>Mit freundlichen Grüßen</p>"
        return EmailService.EmailContent(subject, body)
    }

    private fun mitAnnahmeBeleg(content: EmailService.EmailContent, freigabe: DokumentFreigabe): EmailService.EmailContent {
        val angenommen = freigabe.akzeptiertAm?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) ?: ""
        val beleg = "<div style=\"margin:20px 0;padding:14px 16px;border-left:3px solid #500010;background:#fafafa;font-family:Arial,Helvetica,sans-serif;\">" +
            "<p style=\"margin:0 0 6px 0;font-weight:600;color:#1e293b;\">Angebot digital angenommen - diese Auftragsbestätigung wurde automatisch erstellt.</p>" +
            "<p style=\"margin:0;color:#475569;line-height:1.45;\">Angenommen am: $angenommen</p>" +
            "<p style=\"margin:6px 0 0 0;color:#64748b;font-size:12px;\">Audit-Hash: ${freigabe.hashAcceptance ?: ""}</p>" +
            "</div>"
        return EmailService.EmailContent(content.subject(), beleg + (content.htmlBody() ?: ""))
    }

    private fun ermittleAbsenderAdresse(): String? =
        systemSettingsService.mailFromAddress.takeIf { !it.isNullOrBlank() } ?: systemSettingsService.smtpUsername

    private fun bauePlatzhalterKontext(ab: AusgangsGeschaeftsDokument): Map<String, String> {
        val kunde = effektiverKunde(ab)
        val vorgaenger = ab.vorgaenger
        val ctx = mutableMapOf<String, String>()
        ctx["DOKUMENTNUMMER"] = nullSafe(ab.dokumentNummer)
        ctx["RECHNUNGSNUMMER"] = nullSafe(ab.dokumentNummer)
        ctx["DOKUMENTTYP"] = "Auftragsbestätigung"
        ctx["DATUM"] = ab.datum?.format(DATUM_FORMAT) ?: ""
        ctx["BETREFF"] = nullSafe(ab.betreff)
        ctx["BAUVORHABEN"] = nullSafe(ermittleBauvorhaben(ab))
        ctx["PROJEKTNUMMER"] = nullSafe(ermittleProjektnummer(ab))
        ctx["ZAHLUNGSZIEL_TAGE"] = ab.zahlungszielTage?.toString() ?: "8"
        ctx["ZAHLUNGSZIEL"] = berechneZahlungszielDatum(ab)
        if (kunde != null) {
            ctx["KUNDENNAME"] = nullSafe(kunde.name)
            ctx["KUNDENNUMMER"] = nullSafe(kunde.kundennummer)
            ctx["KUNDENADRESSE"] = nullSafe(baueAdresseAusKunde(kunde))
            ctx["ANSPRECHPARTNER"] = nullSafe(kunde.ansprechspartner)
            ctx["ANREDE"] = kunde.anrede?.toAnredeText() ?: "Sehr geehrte Damen und Herren"
        } else {
            ctx["ANREDE"] = "Sehr geehrte Damen und Herren"
        }
        if (vorgaenger != null) {
            ctx["BEZUGSDOKUMENT"] = nullSafe(vorgaenger.dokumentNummer)
            ctx["BEZUGSDOKUMENTNUMMER"] = nullSafe(vorgaenger.dokumentNummer)
            ctx["BEZUGSDOKUMENTTYP"] = vorgaenger.typ?.let(::typLabel).orEmpty()
            ctx["BEZUGSDOKUMENTDATUM"] = vorgaenger.datum?.format(DATUM_FORMAT) ?: ""
        }
        return ctx
    }

    data class VorlagenDaten(
        val backgroundImagePage1: String?,
        val backgroundImagePage2: String?,
        val formBlocks: List<FormBlockDto>,
    ) {
        fun backgroundImagePage1(): String? = backgroundImagePage1
        fun backgroundImagePage2(): String? = backgroundImagePage2
        fun formBlocks(): List<FormBlockDto> = formBlocks

        companion object {
            @JvmStatic
            fun leer(): VorlagenDaten = VorlagenDaten(null, null, emptyList())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AutoAuftragsbestaetigungVersandService::class.java)
        private val objectMapper = ObjectMapper()
        private val DATUM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private const val MAX_ARCHIV_VERSUCHE = 3

        @JvmStatic
        fun aufloesePlatzhalter(text: String?, ctx: Map<String, String>): String =
            if (text.isNullOrEmpty()) {
                text.orEmpty()
            } else {
                Regex("\\{\\{\\s*([a-zA-Z0-9_äöüÄÖÜß]+)\\s*}}").replace(text) { match ->
                    ctx[match.groupValues[1].uppercase(Locale.GERMAN)] ?: match.value
                }
            }

        private fun loeseBlockAuf(block: ContentBlockDto, ctx: Map<String, String>): ContentBlockDto =
            ContentBlockDto(
                block.type,
                aufloesePlatzhalter(block.text, ctx),
                block.fett,
                block.fontSize,
                block.pos,
                aufloesePlatzhalter(block.beschreibung, ctx),
                aufloesePlatzhalter(block.beschreibungHtml, ctx),
                block.menge,
                block.einheit,
                block.einzelpreis,
                block.gesamt,
                block.optional,
                aufloesePlatzhalter(block.sectionLabel, ctx),
                block.rabattProzent,
            )

        private fun textbausteinAlsBlock(textbaustein: Textbaustein, ctx: Map<String, String>): ContentBlockDto =
            ContentBlockDto(
                "TEXT",
                aufloesePlatzhalter(textbaustein.html ?: "", ctx),
                false,
                10,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
            )

        private fun typLabel(typ: AusgangsGeschaeftsDokumentTyp): String =
            when (typ) {
                AusgangsGeschaeftsDokumentTyp.ANGEBOT -> "Angebot"
                AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT -> "Nachtragsangebot"
                AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG -> "Auftragsbestätigung"
                AusgangsGeschaeftsDokumentTyp.RECHNUNG -> "Rechnung"
                AusgangsGeschaeftsDokumentTyp.TEILRECHNUNG -> "Teilrechnung"
                AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG -> "Abschlagsrechnung"
                AusgangsGeschaeftsDokumentTyp.SCHLUSSRECHNUNG -> "Schlussrechnung"
                AusgangsGeschaeftsDokumentTyp.GUTSCHRIFT -> "Gutschrift"
                AusgangsGeschaeftsDokumentTyp.STORNO -> "Stornorechnung"
                AusgangsGeschaeftsDokumentTyp.ZAHLUNGSERINNERUNG -> "Zahlungserinnerung"
                AusgangsGeschaeftsDokumentTyp.ERSTE_MAHNUNG -> "1. Mahnung"
                AusgangsGeschaeftsDokumentTyp.ZWEITE_MAHNUNG -> "2. Mahnung"
            }

        private fun berechneZahlungszielDatum(ab: AusgangsGeschaeftsDokument): String {
            val tage = ab.zahlungszielTage ?: 8
            val basis = ab.datum ?: LocalDate.now()
            return basis.plusDays(tage.toLong()).format(DATUM_FORMAT)
        }

        private fun effektiverKunde(ab: AusgangsGeschaeftsDokument): Kunde? =
            ab.kunde ?: ab.projekt?.kundenId ?: ab.anfrage?.kunde

        private fun ermittleBauvorhaben(ab: AusgangsGeschaeftsDokument): String? =
            ab.projekt?.bauvorhaben ?: ab.anfrage?.bauvorhaben ?: ab.betreff

        private fun ermittleProjektnummer(ab: AusgangsGeschaeftsDokument): String =
            ab.projekt?.auftragsnummer ?: if (ab.anfrage != null) "-" else ""

        private fun baueAdresseAusKunde(kunde: Kunde): String =
            listOfNotNull(
                kunde.name,
                kunde.strasse,
                listOfNotNull(kunde.plz, kunde.ort).joinToString(" ").takeIf { it.isNotBlank() },
            ).joinToString("\n")

        private fun sanitizeForFilename(input: String?): String =
            input.orEmpty()
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .ifBlank { "ohne_nummer" }

        private fun nullSafe(value: String?): String = value?.trim().orEmpty()

        @JvmStatic
        fun parseVorlagenHtml(html: String?): VorlagenDaten {
            if (html.isNullOrBlank()) return VorlagenDaten.leer()
            val doc = Jsoup.parse(html)
            val page1 = parseMetaContent(doc, "background-image")
                ?: parseMetaContent(doc, "backgroundImagePage1")
            val page2 = parseMetaContent(doc, "background-image-page2")
                ?: parseMetaContent(doc, "backgroundImagePage2")
            val blocks = doc.select("[data-block-type]")
                .mapNotNull { parseFormBlockElement(it) }
            return VorlagenDaten(page1, page2, blocks)
        }

        @JvmStatic
        fun parsePositionenJsonZuContentBlocks(positionenJson: String?): List<RechnungPdfService.ContentBlockDto> {
            if (positionenJson.isNullOrBlank()) return emptyList()
            return try {
                val blocks = readBlocks(positionenJson) ?: return emptyList()
                val result = mutableListOf<RechnungPdfService.ContentBlockDto>()
                val counters = intArrayOf(0)
                blocks.forEach { appendBlock(it, "", counters, result) }
                result
            } catch (e: Exception) {
                log.warn("positionenJson konnte nicht geparst werden: {}", e.message)
                emptyList()
            }
        }

        @JvmStatic
        fun ermittleBruttoBetrag(ab: AusgangsGeschaeftsDokument?): BigDecimal =
            ab?.betragBrutto ?: ab?.betragNetto ?: BigDecimal.ZERO

        @JvmStatic
        fun summiereNettoAusJson(positionenJson: String?): BigDecimal {
            if (positionenJson.isNullOrBlank()) return BigDecimal.ZERO
            return try {
                val blocks = readBlocks(positionenJson) ?: return BigDecimal.ZERO
                blocks.fold(BigDecimal.ZERO) { sum, block -> sum.add(summiereBlock(block)) }
                    .setScale(2, RoundingMode.HALF_UP)
            } catch (e: Exception) {
                log.warn("Netto-Summe konnte aus positionenJson nicht ermittelt werden: {}", e.message)
                BigDecimal.ZERO
            }
        }

        private fun parseMetaContent(doc: org.jsoup.nodes.Document, name: String): String? =
            doc.selectFirst("meta[name=$name]")
                ?.attr("content")
                ?.takeIf { it.isNotBlank() }
                ?.let(::urlDecode)

        private fun parseFormBlockElement(el: Element): FormBlockDto? {
            val type = el.attr("data-block-type").takeIf { it.isNotBlank() } ?: return null
            val id = el.id().takeIf { it.isNotBlank() } ?: "${type}_${kotlin.math.abs(el.hashCode())}"
            val content = if (el.hasAttr("data-content")) urlDecode(el.attr("data-content")) else null
            return FormBlockDto(
                id = id,
                type = type,
                page = parseInt(el.attr("data-page"), 1),
                x = parseFloat(el.attr("data-x"), 0f),
                y = parseFloat(el.attr("data-y"), 0f),
                width = parseFloat(el.attr("data-width"), 0f),
                height = parseFloat(el.attr("data-height"), 0f),
                content = content,
                styles = parseStyleAttribute(el.attr("data-style")),
            )
        }

        private fun parseStyleAttribute(raw: String?): Map<String, Any> {
            if (raw.isNullOrBlank()) return emptyMap()
            return try {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(urlDecode(raw), Map::class.java) as Map<String, Any>
            } catch (_: Exception) {
                emptyMap()
            }
        }

        private fun readBlocks(positionenJson: String): JsonNode? {
            val root = objectMapper.readTree(positionenJson)
            return when {
                root.isArray -> root
                root.has("blocks") && root.get("blocks").isArray -> root.get("blocks")
                else -> null
            }
        }

        private fun appendBlock(
            block: JsonNode,
            parentPos: String,
            counters: IntArray,
            out: MutableList<RechnungPdfService.ContentBlockDto>,
        ) {
            when (optString(block, "type")) {
                "TEXT" -> out += RechnungPdfService.ContentBlockDto(
                    "TEXT",
                    optString(block, "content"),
                    optBoolean(block, "fett", false),
                    optInt(block, "fontSize", 10),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                )
                "SERVICE" -> {
                    counters[0]++
                    val pos = if (parentPos.isBlank()) counters[0].toString() else "$parentPos.${counters[0]}"
                    val menge = optBigDecimal(block, "quantity", BigDecimal.ONE) ?: BigDecimal.ONE
                    val einzelpreis = optBigDecimal(block, "price", BigDecimal.ZERO) ?: BigDecimal.ZERO
                    val rabattProzent = optBigDecimal(block, "discount", null)
                    out += RechnungPdfService.ContentBlockDto(
                        "SERVICE",
                        null,
                        optBoolean(block, "fett", false),
                        optInt(block, "fontSize", 10),
                        pos,
                        optString(block, "title"),
                        optString(block, "description"),
                        menge,
                        optString(block, "unit", "Stk"),
                        einzelpreis,
                        berechneGesamt(menge, einzelpreis, rabattProzent),
                        optBoolean(block, "optional", false),
                        null,
                        rabattProzent,
                    )
                }
                "SECTION_HEADER" -> {
                    counters[0]++
                    val pos = if (parentPos.isBlank()) counters[0].toString() else "$parentPos.${counters[0]}"
                    out += RechnungPdfService.ContentBlockDto(
                        "SECTION_HEADER",
                        null,
                        false,
                        0,
                        pos,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        optString(block, "sectionLabel", ""),
                        null,
                    )
                    val childCounters = intArrayOf(0)
                    block.get("children")
                        ?.takeIf { it.isArray }
                        ?.forEach { appendBlock(it, pos, childCounters, out) }
                }
                "SUBTOTAL", "SEPARATOR", "CLOSURE" -> out += RechnungPdfService.ContentBlockDto(
                    optString(block, "type"),
                    null,
                    false,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                )
            }
        }

        private fun summiereBlock(block: JsonNode): BigDecimal =
            when (optString(block, "type", "")) {
                "SERVICE" -> {
                    if (optBoolean(block, "optional", false)) {
                        BigDecimal.ZERO
                    } else {
                        val menge = optBigDecimal(block, "quantity", BigDecimal.ONE) ?: BigDecimal.ONE
                        val preis = optBigDecimal(block, "price", BigDecimal.ZERO) ?: BigDecimal.ZERO
                        berechneGesamt(menge, preis, optBigDecimal(block, "discount", null))
                    }
                }
                "SECTION_HEADER" -> block.get("children")
                    ?.takeIf { it.isArray }
                    ?.fold(BigDecimal.ZERO) { sum, child -> sum.add(summiereBlock(child)) }
                    ?: BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }

        private fun berechneGesamt(menge: BigDecimal, preis: BigDecimal, rabattProzent: BigDecimal?): BigDecimal {
            var gesamt = menge.multiply(preis)
            if (rabattProzent != null && rabattProzent.signum() > 0) {
                val faktor = BigDecimal.ONE.subtract(
                    rabattProzent.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP),
                )
                gesamt = gesamt.multiply(faktor)
            }
            return gesamt.setScale(2, RoundingMode.HALF_UP)
        }

        private fun optString(node: JsonNode?, key: String, fallback: String? = null): String? =
            node?.takeIf { it.has(key) && !it.get(key).isNull }?.get(key)?.asText() ?: fallback

        private fun optInt(node: JsonNode?, key: String, fallback: Int): Int =
            node?.takeIf { it.has(key) && !it.get(key).isNull }?.get(key)?.asInt(fallback) ?: fallback

        private fun optBoolean(node: JsonNode?, key: String, fallback: Boolean): Boolean =
            node?.takeIf { it.has(key) && !it.get(key).isNull }?.get(key)?.asBoolean(fallback) ?: fallback

        private fun optBigDecimal(node: JsonNode?, key: String, fallback: BigDecimal?): BigDecimal? =
            node?.takeIf { it.has(key) && !it.get(key).isNull }
                ?.get(key)
                ?.asText()
                ?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: fallback

        private fun urlDecode(value: String): String =
            try {
                URLDecoder.decode(value, StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                value
            }

        private fun parseInt(value: String?, fallback: Int): Int =
            value?.toIntOrNull() ?: fallback

        private fun parseFloat(value: String?, fallback: Float): Float =
            value?.toFloatOrNull() ?: fallback
    }
}
