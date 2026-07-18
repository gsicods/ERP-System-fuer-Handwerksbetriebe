package org.example.kalkulationsprogramm.service

import org.example.email.EmailService
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import org.example.kalkulationsprogramm.domain.Firmeninformation
import org.example.kalkulationsprogramm.domain.Kunde
import org.example.kalkulationsprogramm.domain.Mahnstufe
import org.example.kalkulationsprogramm.domain.Projekt
import org.example.kalkulationsprogramm.domain.ProjektGeschaeftsdokument
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository
import org.example.kalkulationsprogramm.repository.FirmeninformationRepository
import org.example.kalkulationsprogramm.repository.ProjektDokumentRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.util.EnumSet
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@Service
open class AutoMahnVersandService(
    private val firmaRepository: FirmeninformationRepository? = null,
    private val projektDokumentRepository: ProjektDokumentRepository? = null,
    private val ausgangsGeschaeftsDokumentRepository: AusgangsGeschaeftsDokumentRepository? = null,
    private val projektEmailArchivService: ProjektEmailArchivService? = null,
    private val dateiSpeicherService: DateiSpeicherService? = null,
    private val rechnungPdfService: RechnungPdfService? = null,
    private val emailTextTemplateService: EmailTextTemplateService? = null,
    private val systemSettingsService: SystemSettingsService? = null,
    private val emailSignatureService: EmailSignatureService? = null,
) {
    enum class MahnlaufStatus {
        AUSGEFUEHRT,
        VERFAHREN_INAKTIV,
        LAEUFT_BEREITS,
    }

    data class MahnlaufErgebnis(
        val status: MahnlaufStatus,
        val versendet: Int,
        val fehlgeschlagen: Int,
    )

    private val laufAktiv = AtomicBoolean(false)

    @Scheduled(cron = "0 0 9 * * *")
    open fun verarbeiteFaelligeMahnungen() {
        fuehreMahnlaufAus()
    }

    open fun fuehreMahnlaufAus(): MahnlaufErgebnis {
        if (!laufAktiv.compareAndSet(false, true)) {
            log.info("Mahn-Lauf uebersprungen: es laeuft bereits einer")
            return MahnlaufErgebnis(MahnlaufStatus.LAEUFT_BEREITS, 0, 0)
        }
        return try {
            fuehreMahnlaufAusIntern()
        } finally {
            laufAktiv.set(false)
        }
    }

    private fun fuehreMahnlaufAusIntern(): MahnlaufErgebnis {
        log.debug("Auto-Mahn-Lauf gestartet")
        val firma = firmaRepository?.findById(1L)?.orElse(null)
            ?: return MahnlaufErgebnis(MahnlaufStatus.VERFAHREN_INAKTIV, 0, 0)
        if (!firma.isMahnverfahrenAktiv()) return MahnlaufErgebnis(MahnlaufStatus.VERFAHREN_INAKTIV, 0, 0)

        var versendet = 0
        var fehlgeschlagen = 0
        val heute = LocalDate.now()
        val offene = projektDokumentRepository?.findOffeneGeschaeftsdokumenteFuerMahnlauf()
            ?: projektDokumentRepository?.findOffeneGeschaeftsdokumente()
            ?: emptyList()
        offene.forEach { dok ->
            try {
                if (verarbeiteRechnung(dok, firma, heute)) versendet++
            } catch (e: Exception) {
                fehlgeschlagen++
                log.error("Auto-Mahn-Lauf fuer Dokument {} fehlgeschlagen: {}", dok.id, e.message, e)
            }
        }
        if (versendet > 0 || fehlgeschlagen > 0) {
            log.info("Auto-Mahn-Lauf abgeschlossen: {} Mahnung(en) versendet, {} fehlgeschlagen", versendet, fehlgeschlagen)
        }
        return MahnlaufErgebnis(MahnlaufStatus.AUSGEFUEHRT, versendet, fehlgeschlagen)
    }

    fun verarbeiteRechnung(dok: ProjektGeschaeftsdokument, firma: Firmeninformation, heute: LocalDate): Boolean {
        if (!istOriginalRechnungOhneMahnstufe(dok)) return false
        if (!dok.isSystemGeneriert()) return false
        if (dok.isBezahlt()) return false
        val faelligkeitsdatum = dok.faelligkeitsdatum ?: return false
        if (dok.projekt == null) return false

        val tageUeberfaellig = ChronoUnit.DAYS.between(faelligkeitsdatum, heute)
        if (tageUeberfaellig <= 0) return false

        val naechsteStufe = ermittleNaechsteStufe(dok, firma, tageUeberfaellig, heute) ?: return false
        val empfaenger = ermittleEmpfaenger(dok)
        if (empfaenger.isNullOrBlank()) {
            log.warn("Auto-Mahnung uebersprungen: keine E-Mail fuer Rechnung {} (Projekt {})", dok.dokumentid, dok.projekt?.id)
            return false
        }
        return erzeugeUndVersende(dok, naechsteStufe, firma, empfaenger, heute, tageUeberfaellig)
    }

    @Transactional
    protected open fun persistiereMahnung(
        rechnung: ProjektGeschaeftsdokument,
        stufe: Mahnstufe,
        pdfBytes: ByteArray,
        heute: LocalDate,
    ): ProjektGeschaeftsdokument {
        val projektId = rechnung.projekt?.id ?: throw IllegalArgumentException("Projekt fehlt fuer Mahnung")
        val mahnNummer = generiereMahnNummer(rechnung, stufe)
        val dateiname = "Mahnung_${stufe.name}_${sanitize(mahnNummer)}.pdf"
        val temp = Files.createTempFile("auto-mahnung-", ".pdf")
        return try {
            Files.write(temp, pdfBytes)
            val dokument = dateiSpeicherService?.speichereZugferdDatei(temp, dateiname, projektId, "Mahnung")
                ?: throw IllegalStateException("DateiSpeicherService nicht verfuegbar")
            dokument.dokumentid = mahnNummer
            dokument.geschaeftsdokumentart = "Mahnung"
            dokument.rechnungsdatum = heute
            dokument.faelligkeitsdatum = heute.plusDays(mahnverfahrenNeuesZielTageSafe(null))
            dokument.bruttoBetrag = rechnung.bruttoBetrag
            dokument.mahnstufe = stufe
            dokument.referenzDokument = rechnung
            dokument.systemGeneriert = true
            projektDokumentRepository?.save(dokument)
            dokument
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    @Transactional
    protected open fun markiereVersendet(dokumentId: Long, datum: LocalDate) {
        val dokument = projektDokumentRepository?.findById(dokumentId)?.orElse(null)
            ?: throw IllegalStateException("Versandtes Mahndokument nicht mehr vorhanden: $dokumentId")
        dokument.emailVersandDatum = datum
        projektDokumentRepository.save(dokument)
    }

    @Transactional(readOnly = true)
    open fun generiereVorschauPdf(rechnungId: Long, stufe: Mahnstufe): ByteArray {
        val rechnung = projektDokumentRepository?.findById(rechnungId)?.orElse(null) as? ProjektGeschaeftsdokument
            ?: throw IllegalArgumentException("Rechnung nicht gefunden: $rechnungId")
        return generiereVorschauPdfIntern(rechnung, stufe)
    }

    @Transactional(readOnly = true)
    open fun generiereVorschauPdfFuerAusgangsRechnung(ausgangsDokumentId: Long, stufe: Mahnstufe): ByteArray {
        val ausgang = ausgangsGeschaeftsDokumentRepository?.findById(ausgangsDokumentId)?.orElse(null)
            ?: throw IllegalArgumentException("Rechnung nicht gefunden")
        val simulation = ProjektGeschaeftsdokument().apply {
            dokumentid = ausgang.dokumentNummer
            geschaeftsdokumentart = labelFuerAusgangsTyp(ausgang.typ)
            bezahlt = false
            bruttoBetrag = ausgang.betragBrutto ?: ausgang.betragNetto
            rechnungsdatum = ausgang.datum
            faelligkeitsdatum = (ausgang.datum ?: LocalDate.now()).plusDays((ausgang.zahlungszielTage ?: 8).toLong())
            projekt = ausgang.projekt ?: Projekt().apply {
                bauvorhaben = ausgang.betreff
                kundenId = ausgang.kunde
            }
        }
        return generiereVorschauPdfIntern(simulation, stufe)
    }

    private fun erzeugeUndVersende(
        rechnung: ProjektGeschaeftsdokument,
        stufe: Mahnstufe,
        firma: Firmeninformation,
        empfaenger: String,
        heute: LocalDate,
        tageUeberfaellig: Long,
    ): Boolean {
        val typLabel = labelFuer(stufe)
        val mahnNummer = generiereMahnNummer(rechnung, stufe)
        val neuesFaelligkeitsdatum = heute.plusDays(mahnverfahrenNeuesZielTageSafe(firma))
        val ctx = bauePlatzhalterKontext(rechnung, typLabel, mahnNummer, heute, neuesFaelligkeitsdatum, tageUeberfaellig)
        val mailInhalt = emailTextTemplateService?.render(stufe.name, ctx)
            ?: EmailService.EmailContent(
                "$typLabel zu Rechnung ${rechnung.dokumentid}",
                fallbackHtml(typLabel, rechnung, neuesFaelligkeitsdatum, tageUeberfaellig),
            )
        val pdfBytes = generierePdf(rechnung, typLabel, mahnNummer, heute, neuesFaelligkeitsdatum, tageUeberfaellig)
        val tempPdf = Files.createTempFile("auto-mahnung-", ".pdf")
        var smtpVersendet = false
        var versandMarkiert = false
        return try {
            Files.write(tempPdf, pdfBytes)
            val gespeichert = persistiereMahnung(rechnung, stufe, pdfBytes, heute).apply {
                faelligkeitsdatum = neuesFaelligkeitsdatum
                projektDokumentRepository?.save(this)
            }
            val absender = systemSettingsService?.mailFromAddress
                ?: throw IllegalStateException("SystemSettingsService nicht verfuegbar")
            val smtp = systemSettingsService
            val htmlMitSignatur = emailSignatureService?.appendSystemSignatureIfConfigured(mailInhalt.htmlBody())
                ?: mailInhalt.htmlBody()
            val dateiname = "Mahnung_${stufe.name}_${sanitize(mahnNummer)}.pdf"
            val messageId = EmailService(
                smtp.smtpHost,
                smtp.smtpPort,
                smtp.smtpUsername,
                smtp.smtpPassword,
            ).sendEmailAndReturnMessageId(empfaenger, null, absender, mailInhalt.subject(), htmlMitSignatur, tempPdf.toString(), dateiname)
            smtpVersendet = true
            markiereVersendet(gespeichert.id ?: throw IllegalStateException("Mahndokument ohne ID"), heute)
            versandMarkiert = true
            val projekt = rechnung.projekt
            if (projekt != null) {
                projektEmailArchivService?.archiviereVersandteEmail(
                    projekt,
                    empfaenger,
                    absender,
                    mailInhalt.subject(),
                    htmlMitSignatur,
                    messageId,
                    tempPdf,
                    dateiname,
                )
            }
            log.info("Auto-Mahnung [{}] {} fuer Rechnung {} versendet", typLabel, mahnNummer, rechnung.dokumentid)
            true
        } catch (e: Exception) {
            if (smtpVersendet) {
                val status = if (versandMarkiert) {
                    "Die Mahnstufe bleibt als versendet markiert und wird nicht erneut gesendet."
                } else {
                    "Die Versandmarkierung konnte nicht bestaetigt werden; manueller Eingriff ist erforderlich."
                }
                throw IllegalStateException("E-Mail der Stufe $typLabel fuer Rechnung ${rechnung.dokumentid} wurde per SMTP versendet, aber nicht vollstaendig im ERP archiviert. $status", e)
            }
            throw IllegalStateException("Versand der Stufe $typLabel fuer Rechnung ${rechnung.dokumentid} fehlgeschlagen: ${e.message}", e)
        } finally {
            Files.deleteIfExists(tempPdf)
        }
    }

    private fun generiereVorschauPdfIntern(rechnung: ProjektGeschaeftsdokument, stufe: Mahnstufe): ByteArray {
        val heute = LocalDate.now()
        val firma = firmaRepository?.findById(1L)?.orElse(null)
        val neuesFaelligkeitsdatum = heute.plusDays(mahnverfahrenNeuesZielTageSafe(firma))
        val tageUeberfaellig = rechnung.faelligkeitsdatum
            ?.let { ChronoUnit.DAYS.between(it, heute).coerceAtLeast(1) }
            ?: 1
        return generierePdf(rechnung, labelFuer(stufe), generiereMahnNummer(rechnung, stufe), heute, neuesFaelligkeitsdatum, tageUeberfaellig)
    }

    private fun generierePdf(
        rechnung: ProjektGeschaeftsdokument,
        typLabel: String,
        mahnNummer: String,
        heute: LocalDate,
        neuesFaelligkeitsdatum: LocalDate,
        tageUeberfaellig: Long,
    ): ByteArray {
        val kunde = rechnung.projekt?.kundenId
        val dto = RechnungPdfService.RechnungDto(
            RechnungPdfService.getDefaultLayout(),
            RechnungPdfService.KopfdatenDto(
                mahnNummer,
                heute,
                heute,
                kunde?.name,
                baueAdresse(kunde),
                "$typLabel - Rechnung ${rechnung.dokumentid.orEmpty()}",
                kunde?.kundennummer,
                typLabel,
                rechnung.dokumentid,
                rechnung.projekt?.auftragsnummer,
                rechnung.projekt?.bauvorhaben,
                "Rechnung",
                rechnung.rechnungsdatum?.format(DATUM_FORMAT),
                null,
            ),
            listOf(
                textBlock(buildEinleitungsBlock(typLabel, rechnung, tageUeberfaellig)),
                textBlock(buildForderungsBlock(rechnung, tageUeberfaellig, neuesFaelligkeitsdatum)),
                textBlock(buildSchlussBlock(typLabel, neuesFaelligkeitsdatum)),
            ),
            emptyList(),
            null,
            null,
            null,
        )
        return rechnungPdfService?.generatePdfBytes(dto)
            ?: throw IllegalStateException("RechnungPdfService nicht verfuegbar")
    }

    companion object {
        private val log = LoggerFactory.getLogger(AutoMahnVersandService::class.java)
        private val DATUM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        fun ermittleNaechsteStufe(
            rechnung: ProjektGeschaeftsdokument,
            firma: Firmeninformation,
            tageUeberfaellig: Long,
            heute: LocalDate,
        ): Mahnstufe? {
            val bereits = bereitsVersendeteStufen(rechnung)
            if (!bereits.contains(Mahnstufe.ZAHLUNGSERINNERUNG)) {
                return if (tageUeberfaellig >= firma.tageBisZahlungserinnerung) Mahnstufe.ZAHLUNGSERINNERUNG else null
            }
            if (!bereits.contains(Mahnstufe.ERSTE_MAHNUNG)) {
                return if (istAbstandSeitVorstufeErreicht(rechnung, Mahnstufe.ZAHLUNGSERINNERUNG, firma.tageBisErsteMahnung, heute)) {
                    Mahnstufe.ERSTE_MAHNUNG
                } else {
                    null
                }
            }
            if (!bereits.contains(Mahnstufe.ZWEITE_MAHNUNG)) {
                return if (istAbstandSeitVorstufeErreicht(rechnung, Mahnstufe.ERSTE_MAHNUNG, firma.tageBisZweiteMahnung, heute)) {
                    Mahnstufe.ZWEITE_MAHNUNG
                } else {
                    null
                }
            }
            return null
        }

        private fun istOriginalRechnungOhneMahnstufe(dok: ProjektGeschaeftsdokument): Boolean {
            if (dok.mahnstufe != null) return false
            val art = dok.geschaeftsdokumentart?.lowercase(Locale.GERMAN) ?: return false
            return art.contains("rechnung") && !art.contains("mahn")
        }

        private fun bereitsVersendeteStufen(rechnung: ProjektGeschaeftsdokument): MutableSet<Mahnstufe> {
            val result = EnumSet.noneOf(Mahnstufe::class.java)
            rechnung.mahnungen.forEach { it.mahnstufe?.let(result::add) }
            return result
        }

        private fun istAbstandSeitVorstufeErreicht(
            rechnung: ProjektGeschaeftsdokument,
            vorstufe: Mahnstufe,
            abstandTage: Int,
            heute: LocalDate,
        ): Boolean {
            val vorstufenDatum = datumDerStufe(rechnung, vorstufe, heute)
            return !heute.isBefore(vorstufenDatum.plusDays(abstandTage.coerceAtLeast(1).toLong()))
        }

        private fun datumDerStufe(rechnung: ProjektGeschaeftsdokument, stufe: Mahnstufe, heute: LocalDate): LocalDate =
            rechnung.mahnungen
                .filter { it.mahnstufe == stufe }
                .map { it.emailVersandDatum ?: it.rechnungsdatum ?: it.uploadDatum ?: heute }
                .maxOrNull()
                ?: heute

        private fun ermittleEmpfaenger(rechnung: ProjektGeschaeftsdokument): String? {
            val projekt = rechnung.projekt ?: return null
            val candidates = projekt.kundenEmails + projekt.kundenId?.kundenEmails.orEmpty()
            return candidates.firstOrNull { it.isNotBlank() }?.trim()
        }

        private fun labelFuer(stufe: Mahnstufe): String = stufe.beschreibung

        private fun labelFuerAusgangsTyp(typ: AusgangsGeschaeftsDokumentTyp?): String =
            when (typ) {
                AusgangsGeschaeftsDokumentTyp.TEILRECHNUNG -> "Teilrechnung"
                AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG -> "Abschlagsrechnung"
                AusgangsGeschaeftsDokumentTyp.SCHLUSSRECHNUNG -> "Schlussrechnung"
                else -> "Rechnung"
            }

        private fun generiereMahnNummer(rechnung: ProjektGeschaeftsdokument, stufe: Mahnstufe): String {
            val basis = rechnung.dokumentid?.takeIf { it.isNotBlank() } ?: "Rechnung-${rechnung.id ?: "neu"}"
            val suffix = when (stufe) {
                Mahnstufe.ZAHLUNGSERINNERUNG -> "ZE"
                Mahnstufe.ERSTE_MAHNUNG -> "M1"
                Mahnstufe.ZWEITE_MAHNUNG -> "M2"
            }
            return "$basis-$suffix"
        }

        private fun mahnverfahrenNeuesZielTageSafe(firma: Firmeninformation?): Long =
            (firma?.mahnverfahrenNeuesZahlungszielTage ?: 7).coerceAtLeast(1).toLong()

        private fun bauePlatzhalterKontext(
            rechnung: ProjektGeschaeftsdokument,
            typLabel: String,
            mahnNummer: String,
            heute: LocalDate,
            neuesFaelligkeitsdatum: LocalDate,
            tageUeberfaellig: Long,
        ): Map<String, String> {
            val kunde = rechnung.projekt?.kundenId
            return mapOf(
                "DOKUMENTTYP" to typLabel,
                "MAHNNUMMER" to mahnNummer,
                "RECHNUNGSNUMMER" to rechnung.dokumentid.orEmpty(),
                "RECHNUNGSDATUM" to (rechnung.rechnungsdatum?.format(DATUM_FORMAT).orEmpty()),
                "FAELLIGKEITSDATUM" to (rechnung.faelligkeitsdatum?.format(DATUM_FORMAT).orEmpty()),
                "NEUES_FAELLIGKEITSDATUM" to neuesFaelligkeitsdatum.format(DATUM_FORMAT),
                "MAHNDATUM" to heute.format(DATUM_FORMAT),
                "TAGE_UEBERFAELLIG" to tageUeberfaellig.toString(),
                "BETRAG" to formatBetrag(rechnung.bruttoBetrag),
                "KUNDENNAME" to kunde?.name.orEmpty(),
                "KUNDENNUMMER" to kunde?.kundennummer.orEmpty(),
                "KUNDENADRESSE" to baueAdresse(kunde).orEmpty(),
                "PROJEKTNUMMER" to rechnung.projekt?.auftragsnummer.orEmpty(),
                "BAUVORHABEN" to rechnung.projekt?.bauvorhaben.orEmpty(),
            )
        }

        private fun textBlock(text: String): RechnungPdfService.ContentBlockDto =
            RechnungPdfService.ContentBlockDto("TEXT", text, false, 10, null, null, null, null, null, null, null, false, null, null)

        private fun buildEinleitungsBlock(typLabel: String, rechnung: ProjektGeschaeftsdokument, tageUeberfaellig: Long): String =
            "Unsere Rechnung ${rechnung.dokumentid.orEmpty()} ist seit $tageUeberfaellig Tag(en) faellig. Bitte begleichen Sie den offenen Betrag."

        private fun buildForderungsBlock(rechnung: ProjektGeschaeftsdokument, tageUeberfaellig: Long, neuesFaelligkeitsdatum: LocalDate): String =
            """
            Rechnung: ${rechnung.dokumentid.orEmpty()}
            Rechnungsdatum: ${rechnung.rechnungsdatum?.format(DATUM_FORMAT).orEmpty()}
            Faellig seit: ${rechnung.faelligkeitsdatum?.format(DATUM_FORMAT).orEmpty()}
            Ueberfaellig: $tageUeberfaellig Tag(e)
            Offener Betrag: ${formatBetrag(rechnung.bruttoBetrag)}
            Neues Zahlungsziel: ${neuesFaelligkeitsdatum.format(DATUM_FORMAT)}
            """.trimIndent()

        private fun buildSchlussBlock(typLabel: String, neuesFaelligkeitsdatum: LocalDate): String =
            "Bitte zahlen Sie den Betrag bis zum ${neuesFaelligkeitsdatum.format(DATUM_FORMAT)}. Sollten Sie bereits gezahlt haben, betrachten Sie diese $typLabel als gegenstandslos."

        private fun fallbackHtml(typLabel: String, rechnung: ProjektGeschaeftsdokument, neuesFaelligkeitsdatum: LocalDate, tageUeberfaellig: Long): String =
            buildString {
                append("<p>Sehr geehrte Damen und Herren,</p>")
                append("<p>")
                append(buildEinleitungsBlock(typLabel, rechnung, tageUeberfaellig))
                append("</p><p>Neues Zahlungsziel: ")
                append(neuesFaelligkeitsdatum.format(DATUM_FORMAT))
                append("</p>")
            }

        private fun baueAdresse(kunde: Kunde?): String? {
            if (kunde == null) return null
            return listOfNotNull(
                kunde.name?.takeIf { it.isNotBlank() },
                kunde.strasse?.takeIf { it.isNotBlank() },
                listOfNotNull(kunde.plz, kunde.ort).joinToString(" ").takeIf { it.isNotBlank() },
            ).joinToString("\n").takeIf { it.isNotBlank() }
        }

        private fun formatBetrag(betrag: BigDecimal?): String =
            if (betrag == null) "" else "%,.2f EUR".format(Locale.GERMANY, betrag)

        private fun sanitize(value: String): String =
            value.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").ifBlank { "Dokument" }
    }
}
