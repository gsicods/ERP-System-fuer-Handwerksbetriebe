package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.kalkulationsprogramm.domain.AnfrageGeschaeftsdokument
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import org.example.kalkulationsprogramm.domain.DokumentFreigabe
import org.example.kalkulationsprogramm.domain.FreigabeQuellTyp
import org.example.kalkulationsprogramm.domain.FreigabeStatus
import org.example.kalkulationsprogramm.domain.ProjektGeschaeftsdokument
import org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument.AusgangsGeschaeftsDokumentErstellenDto
import org.example.kalkulationsprogramm.dto.Freigabe.FreigabeAuditDto
import org.example.kalkulationsprogramm.dto.Freigabe.FreigabePositionDto
import org.example.kalkulationsprogramm.repository.AnfrageDokumentRepository
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository
import org.example.kalkulationsprogramm.repository.DokumentFreigabeRepository
import org.example.kalkulationsprogramm.repository.ProjektDokumentRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.HexFormat
import java.util.Locale
import java.util.Optional
import java.util.UUID

@Service
open class DokumentFreigabeService(
    private val repository: DokumentFreigabeRepository,
    private val anfrageDokumentRepository: AnfrageDokumentRepository,
    private val projektDokumentRepository: ProjektDokumentRepository,
    private val ausgangsGeschaeftsDokumentRepository: AusgangsGeschaeftsDokumentRepository,
    private val ausgangsGeschaeftsDokumentService: AusgangsGeschaeftsDokumentService,
    private val ausgangsGeschaeftsDokumentAuditService: AusgangsGeschaeftsDokumentAuditService,
    private val webPushService: WebPushService,
    private val dateiSpeicherService: DateiSpeicherService,
    private val autoAuftragsbestaetigungVersandService: AutoAuftragsbestaetigungVersandService,
    private val taskExecutor: TaskExecutor,
) {
    @Value("\${freigabe.hash.salt:CHANGE_ME_LOCAL_ONLY}")
    private lateinit var hashSalt: String

    @Value("\${freigabe.public-base-url:https://bauschlosserei-kuhn.de}")
    private lateinit var publicBaseUrl: String

    @Transactional
    open fun erstelleFuerAnfrage(
        dokument: AnfrageGeschaeftsdokument,
        kundeName: String?,
        kundeEmail: String?,
    ): DokumentFreigabe = erstelleFuerAnfrage(dokument, kundeName, kundeEmail, DEFAULT_GUELTIGKEITS_TAGE)

    @Transactional
    open fun erstelleFuerAnfrage(
        dokument: AnfrageGeschaeftsdokument,
        kundeName: String?,
        kundeEmail: String?,
        gueltigkeitTage: Int,
    ): DokumentFreigabe {
        revokeAltePendingFreigaben(FreigabeQuellTyp.ANFRAGE, dokument.id)
        val freigabe = baseFreigabe(gueltigkeitTage).apply {
            quellTyp = FreigabeQuellTyp.ANFRAGE
            quellDokumentId = dokument.id
            dokumentNummer = dokument.dokumentid
            dokumentArt = dokument.geschaeftsdokumentart
            dokumentBetrag = dokument.bruttoBetrag
            dokumentDatei = dokument.gespeicherterDateiname
            bauvorhaben = dokument.anfrage?.bauvorhaben
            this.kundeName = kundeName
            this.kundeEmail = kundeEmail
        }
        freigabe.hashOriginal = berechneHashOriginal(freigabe)
        return repository.save(freigabe)
    }

    @Transactional
    open fun erstelleFuerProjekt(
        dokument: ProjektGeschaeftsdokument,
        kundeName: String?,
        kundeEmail: String?,
    ): DokumentFreigabe = erstelleFuerProjekt(dokument, kundeName, kundeEmail, DEFAULT_GUELTIGKEITS_TAGE)

    @Transactional
    open fun erstelleFuerProjekt(
        dokument: ProjektGeschaeftsdokument,
        kundeName: String?,
        kundeEmail: String?,
        gueltigkeitTage: Int,
    ): DokumentFreigabe {
        revokeAltePendingFreigaben(FreigabeQuellTyp.PROJEKT, dokument.id)
        val freigabe = baseFreigabe(gueltigkeitTage).apply {
            quellTyp = FreigabeQuellTyp.PROJEKT
            quellDokumentId = dokument.id
            dokumentNummer = dokument.dokumentid
            dokumentArt = dokument.geschaeftsdokumentart
            dokumentBetrag = dokument.bruttoBetrag
            dokumentDatei = dokument.gespeicherterDateiname
            bauvorhaben = dokument.projekt?.bauvorhaben
            this.kundeName = kundeName
            this.kundeEmail = kundeEmail
        }
        freigabe.hashOriginal = berechneHashOriginal(freigabe)
        return repository.save(freigabe)
    }

    @Transactional
    open fun erstelleFuerAusgangsGeschaeftsDokument(
        dok: AusgangsGeschaeftsDokument,
        kundeEmail: String?,
        pdfDateiname: String?,
    ): DokumentFreigabe = erstelleFuerAusgangsGeschaeftsDokument(dok, kundeEmail, pdfDateiname, DEFAULT_GUELTIGKEITS_TAGE)

    @Transactional
    open fun erstelleFuerAusgangsGeschaeftsDokument(
        dok: AusgangsGeschaeftsDokument,
        kundeEmail: String?,
        pdfDateiname: String?,
        gueltigkeitTage: Int,
    ): DokumentFreigabe {
        revokeAltePendingFreigaben(FreigabeQuellTyp.AUSGANGS_DOKUMENT, dok.id)
        val kundeName = dok.projekt?.kundenId?.name ?: dok.anfrage?.kunde?.name ?: dok.kunde?.name
        val freigabe = baseFreigabe(gueltigkeitTage).apply {
            quellTyp = FreigabeQuellTyp.AUSGANGS_DOKUMENT
            quellDokumentId = dok.id
            dokumentNummer = dok.dokumentNummer
            dokumentArt = typZuBezeichnung(dok.typ)
            dokumentBetrag = dok.betragBrutto
            dokumentDatei = pdfDateiname
            bauvorhaben = dok.projekt?.bauvorhaben ?: dok.anfrage?.bauvorhaben
            this.kundeName = kundeName
            this.kundeEmail = kundeEmail
            positionenSnapshot = dok.positionenJson
            basisNetto = dok.betragNetto
            mwstSatz = dok.mwstSatz
        }
        freigabe.hashOriginal = berechneHashOriginal(freigabe)
        return repository.save(freigabe)
    }

    open fun buildPublicUrl(freigabe: DokumentFreigabe): String =
        publicBaseUrl.trimEnd('/') + "/freigabe/${freigabe.uuid}"

    @Transactional
    open fun loeschePdfFuerFreigabe(uuid: String) {
        repository.findByUuid(uuid).ifPresent { freigabe ->
            val dateiname = freigabe.dokumentDatei
            if (!dateiname.isNullOrBlank()) {
                dateiSpeicherService.loescheDokumentPdfByDateiname(dateiname)
                freigabe.dokumentDatei = null
                repository.save(freigabe)
            }
        }
    }

    @Transactional
    open fun erstelleFreigabeBlockFuerDokument(
        dokumentId: Long,
        isAnfrage: Boolean,
        recipient: String?,
        pdfDateiname: String?,
    ): Optional<String> =
        erstelleFreigabeBlockFuerDokument(dokumentId, isAnfrage, recipient, pdfDateiname, DEFAULT_GUELTIGKEITS_TAGE)

    @Transactional
    open fun erstelleFreigabeBlockFuerDokument(
        dokumentId: Long,
        isAnfrage: Boolean,
        recipient: String?,
        pdfDateiname: String?,
        gueltigkeitTage: Int,
    ): Optional<String> {
        val tage = clampGueltigkeitTage(gueltigkeitTage)
        return try {
            ausgangsGeschaeftsDokumentRepository.findById(dokumentId).orElse(null)?.let { dokument ->
                if (!istAngebotOderABTyp(dokument.typ)) return Optional.empty()
                val freigabe = erstelleFuerAusgangsGeschaeftsDokument(dokument, recipient, pdfDateiname, tage)
                return Optional.of(buildFreigabeBlockHtml(buildPublicUrl(freigabe), typZuBezeichnung(dokument.typ), tage, freigabe.ablaufDatum))
            }

            if (isAnfrage) {
                anfrageDokumentRepository.findById(dokumentId)
                    .filter { it is AnfrageGeschaeftsdokument }
                    .map { it as AnfrageGeschaeftsdokument }
                    .filter { istAngebotOderAB(it.geschaeftsdokumentart) }
                    .map {
                        val freigabe = erstelleFuerAnfrage(it, it.anfrage?.kunde?.name, recipient, tage)
                        buildFreigabeBlockHtml(buildPublicUrl(freigabe), it.geschaeftsdokumentart, tage, freigabe.ablaufDatum)
                    }
            } else {
                projektDokumentRepository.findById(dokumentId)
                    .filter { it is ProjektGeschaeftsdokument }
                    .map { it as ProjektGeschaeftsdokument }
                    .filter { istAngebotOderAB(it.geschaeftsdokumentart) }
                    .map {
                        val freigabe = erstelleFuerProjekt(it, it.projekt?.kundenId?.name, recipient, tage)
                        buildFreigabeBlockHtml(buildPublicUrl(freigabe), it.geschaeftsdokumentart, tage, freigabe.ablaufDatum)
                    }
            }
        } catch (ex: Exception) {
            log.warn("Freigabe-Block konnte nicht erstellt werden (dokumentId={}): {}", dokumentId, ex.message)
            Optional.empty()
        }
    }

    @Transactional
    open fun findByUuidUndAktualisiereStatus(uuid: String): Optional<DokumentFreigabe> =
        repository.findByUuid(uuid).map { freigabe ->
            if (freigabe.status == FreigabeStatus.PENDING && freigabe.istAbgelaufen()) {
                freigabe.status = FreigabeStatus.EXPIRED
                repository.save(freigabe)
            }
            freigabe
        }

    @Transactional
    open fun akzeptiere(uuid: String, ip: String?, userAgent: String?, email: String?, name: String?): DokumentFreigabe {
        val parts = normalisiereName(name)?.split(" ", limit = 2).orEmpty()
        return akzeptiere(uuid, ip, userAgent, email, parts.getOrNull(0), parts.getOrNull(1), name, null)
    }

    @Transactional
    open fun akzeptiere(
        uuid: String,
        ip: String?,
        userAgent: String?,
        email: String?,
        name: String?,
        ausgewaehlteAlternativIds: List<String>?,
    ): DokumentFreigabe {
        val parts = normalisiereName(name)?.split(" ", limit = 2).orEmpty()
        return akzeptiere(uuid, ip, userAgent, email, parts.getOrNull(0), parts.getOrNull(1), name, ausgewaehlteAlternativIds)
    }

    @Transactional
    open fun akzeptiere(
        uuid: String,
        ip: String?,
        userAgent: String?,
        email: String?,
        vorname: String?,
        nachname: String?,
        unterzeichnerName: String?,
        ausgewaehlteAlternativIds: List<String>?,
    ): DokumentFreigabe {
        val freigabe = repository.findByUuid(uuid).orElseThrow { IllegalArgumentException(UNBEKANNTE_UUID_MESSAGE) }
        if (freigabe.status == FreigabeStatus.ACCEPTED) return freigabe
        if (freigabe.status != FreigabeStatus.PENDING) throw IllegalStateException("Freigabe ist nicht mehr gueltig: ${freigabe.status}")
        if (freigabe.istAbgelaufen()) {
            freigabe.status = FreigabeStatus.EXPIRED
            repository.save(freigabe)
            throw IllegalStateException("Freigabe ist abgelaufen")
        }

        val vornameNorm = normalisiereName(vorname)
        val nachnameNorm = normalisiereName(nachname)
        var anzeigeName = normalisiereName(unterzeichnerName)
        if (vornameNorm.isNullOrBlank() || nachnameNorm.isNullOrBlank()) {
            throw IllegalArgumentException("Vor- und Nachname sind fuer die digitale Annahme erforderlich.")
        }
        if (anzeigeName.isNullOrBlank()) {
            anzeigeName = "$vornameNorm $nachnameNorm".trim()
        }

        val auswahl = loeseAlternativAuswahl(freigabe, ausgewaehlteAlternativIds)
        val jetzt = LocalDateTime.now()
        freigabe.status = FreigabeStatus.ACCEPTED
        freigabe.akzeptiertAm = jetzt
        freigabe.akzeptiertIp = ip
        freigabe.akzeptiertUserAgent = userAgent
        freigabe.akzeptiertEmail = email
        freigabe.unterzeichnerVorname = vornameNorm
        freigabe.unterzeichnerNachname = nachnameNorm
        freigabe.unterzeichnerName = anzeigeName
        freigabe.akzeptierteAlternativen = auswahl.json
        freigabe.akzeptierterBetrag = auswahl.betragBrutto
        freigabe.hashAcceptance = berechneHashAcceptance(freigabe, ip, email, jetzt)
        val saved = repository.save(freigabe)

        notifyAndCreateFollowUp(saved, auswahl.idSet)
        return saved
    }

    @Transactional(readOnly = true)
    open fun ladePositionsAnsicht(f: DokumentFreigabe): FreigabePositionsAnsicht? {
        if (f.quellTyp != FreigabeQuellTyp.AUSGANGS_DOKUMENT || f.quellDokumentId == null) return null
        var json = f.positionenSnapshot
        var basisNetto = f.basisNetto
        var basisBrutto = f.dokumentBetrag
        var mwst = f.mwstSatz
        if (json == null) {
            val dokument = ausgangsGeschaeftsDokumentRepository.findById(f.quellDokumentId!!).orElse(null) ?: return null
            json = dokument.positionenJson ?: return null
            basisNetto = dokument.betragNetto
            basisBrutto = dokument.betragBrutto
            mwst = dokument.mwstSatz
        }
        val positionen = ausgangsGeschaeftsDokumentService.baueKundenPositionen(json)
        val hatAlternativen = ausgangsGeschaeftsDokumentService.sammleOptionaleAlternativIds(json).isNotEmpty()
        return FreigabePositionsAnsicht(
            positionen,
            basisNetto,
            basisBrutto,
            mwst?.multiply(BigDecimal("100")),
            hatAlternativen,
            null,
            f.akzeptierteAlternativen,
        )
    }

    @Transactional(readOnly = true)
    open fun findAuditByQuelle(typ: FreigabeQuellTyp, quellDokumentId: Long): Optional<FreigabeAuditDto> {
        val freigabe = findJuengsteProQuelle(typ, listOf(quellDokumentId))[quellDokumentId] ?: return Optional.empty()
        return Optional.of(
            FreigabeAuditDto.builder()
                .status(freigabe.status?.name)
                .dokumentArt(freigabe.dokumentArt)
                .dokumentNummer(freigabe.dokumentNummer)
                .erstelltAm(freigabe.erstelltAm)
                .ablaufDatum(freigabe.ablaufDatum)
                .akzeptiertAm(freigabe.akzeptiertAm)
                .akzeptiertEmail(freigabe.akzeptiertEmail)
                .akzeptiertIp(freigabe.akzeptiertIp)
                .akzeptiertUserAgent(freigabe.akzeptiertUserAgent)
                .unterzeichnerVorname(freigabe.unterzeichnerVorname)
                .unterzeichnerNachname(freigabe.unterzeichnerNachname)
                .unterzeichnerName(freigabe.unterzeichnerName)
                .hashOriginal(freigabe.hashOriginal)
                .hashAcceptance(freigabe.hashAcceptance)
                .build(),
        )
    }

    @Transactional(readOnly = true)
    open fun findJuengsteProQuelle(typ: FreigabeQuellTyp, quellDokumentIds: List<Long>): Map<Long, DokumentFreigabe> {
        if (quellDokumentIds.isEmpty()) return emptyMap()
        return repository.findByQuelle(typ, quellDokumentIds).fold(mutableMapOf()) { acc, freigabe ->
            freigabe.effektivenStatusSetzen()
            val id = freigabe.quellDokumentId ?: return@fold acc
            acc.merge(id, freigabe, ::pickRelevant)
            acc
        }
    }

    @Transactional(readOnly = true)
    open fun findJuengsteProAnfrage(anfrageIds: List<Long>): Map<Long, DokumentFreigabe> {
        if (anfrageIds.isEmpty()) return emptyMap()
        val alt = aggregiereProContainer(FreigabeQuellTyp.ANFRAGE, anfrageDokumentRepository.findGeschaeftsdokumentIdMappingByAnfrageIds(anfrageIds))
        val neu = aggregiereProContainer(FreigabeQuellTyp.AUSGANGS_DOKUMENT, ausgangsGeschaeftsDokumentRepository.findIdAnfrageIdMappingByAnfrageIds(anfrageIds))
        return mergeFreigabenProContainer(alt, neu)
    }

    @Transactional(readOnly = true)
    open fun findJuengsteProProjekt(projektIds: List<Long>): Map<Long, DokumentFreigabe> {
        if (projektIds.isEmpty()) return emptyMap()
        val alt = aggregiereProContainer(FreigabeQuellTyp.PROJEKT, projektDokumentRepository.findGeschaeftsdokumentIdMappingByProjektIds(projektIds))
        val neu = aggregiereProContainer(FreigabeQuellTyp.AUSGANGS_DOKUMENT, ausgangsGeschaeftsDokumentRepository.findIdProjektIdMappingByProjektIds(projektIds))
        return mergeFreigabenProContainer(alt, neu)
    }

    private fun notifyAndCreateFollowUp(freigabe: DokumentFreigabe, ausgewaehlteAlternativen: Set<String>) {
        try {
            val art = freigabe.dokumentArt ?: "Dokument"
            val kunde = freigabe.kundeName?.takeIf { it.isNotBlank() } ?: freigabe.kundeEmail ?: "Ein Kunde"
            val body = "${freigabe.unterzeichnerName ?: kunde} hat $art ${freigabe.dokumentNummer} digital angenommen."
            webPushService.notifyFreigabeAnnahme("$art angenommen", body, "/zeiterfassung/projekte")
        } catch (_: Exception) {
        }

        try {
            erzeugeAutoAuftragsbestaetigungWennAngebot(freigabe, ausgewaehlteAlternativen)
        } catch (ex: Exception) {
            log.warn("Auto-Auftragsbestaetigung nach digitaler Annahme fehlgeschlagen (Freigabe {})", freigabe.uuid, ex)
        }
    }

    private fun erzeugeAutoAuftragsbestaetigungWennAngebot(freigabe: DokumentFreigabe, ausgewaehlteAlternativen: Set<String>) {
        if (freigabe.quellTyp != FreigabeQuellTyp.AUSGANGS_DOKUMENT) return
        val angebotId = freigabe.quellDokumentId ?: return
        var angebot = ausgangsGeschaeftsDokumentRepository.findById(angebotId).orElse(null) ?: return
        if (angebot.typ != AusgangsGeschaeftsDokumentTyp.ANGEBOT && angebot.typ != AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT) return

        if (!angebot.digitalAngenommen) {
            angebot.digitalAngenommen = true
            angebot = ausgangsGeschaeftsDokumentRepository.save(angebot)
            ausgangsGeschaeftsDokumentAuditService.protokolliereDigitaleAnnahme(angebot, freigabe.akzeptiertIp)
        }
        if (angebot.nachfolger.any { it.typ == AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG && !it.storniert }) return

        val dto = AusgangsGeschaeftsDokumentErstellenDto(
            typ = AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG,
            vorgaengerId = angebotId,
        )
        val snapshotJson = freigabe.positionenSnapshot ?: angebot.positionenJson
        if (ausgewaehlteAlternativen.isNotEmpty() && snapshotJson != null) {
            dto.positionenJson = ausgangsGeschaeftsDokumentService.markiereAlternativenAlsBeauftragt(
                ausgangsGeschaeftsDokumentService.bereitePositionenFuerTypwechsel(snapshotJson),
                ausgewaehlteAlternativen,
            )
            val basisNetto = freigabe.basisNetto ?: angebot.betragNetto ?: BigDecimal.ZERO
            dto.betragNetto = basisNetto.add(ausgangsGeschaeftsDokumentService.summeAusgewaehlterAlternativenNetto(snapshotJson, ausgewaehlteAlternativen))
            dto.mwstSatz = freigabe.mwstSatz ?: angebot.mwstSatz
        }

        val ab = ausgangsGeschaeftsDokumentService.erstellen(dto)
        val abId = ab.id ?: return
        val empfaenger = freigabe.kundeEmail?.takeIf { it.isNotBlank() } ?: return
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        taskExecutor.execute {
                            try {
                                autoAuftragsbestaetigungVersandService.versendeNachAnnahme(abId, empfaenger, freigabe.uuid)
                            } catch (ex: Exception) {
                                log.error("Auto-AB-Versand nach Annahme fehlgeschlagen (abId={}): {}", abId, ex.message, ex)
                            }
                        }
                    }
                },
            )
        } else {
            autoAuftragsbestaetigungVersandService.versende(ab, empfaenger, freigabe)
        }
    }

    private fun loeseAlternativAuswahl(freigabe: DokumentFreigabe, angefragt: List<String>?): AlternativAuswahl {
        if (angefragt.isNullOrEmpty() || freigabe.quellTyp != FreigabeQuellTyp.AUSGANGS_DOKUMENT || freigabe.quellDokumentId == null) {
            return AlternativAuswahl.LEER
        }
        var json = freigabe.positionenSnapshot
        var basisNetto = freigabe.basisNetto
        var mwst = freigabe.mwstSatz
        if (json == null) {
            val dokument = ausgangsGeschaeftsDokumentRepository.findById(freigabe.quellDokumentId!!).orElse(null) ?: return AlternativAuswahl.LEER
            json = dokument.positionenJson ?: return AlternativAuswahl.LEER
            basisNetto = dokument.betragNetto
            mwst = dokument.mwstSatz
        }

        val erlaubt = ausgangsGeschaeftsDokumentService.sammleOptionaleAlternativIds(json)
        val gueltig = angefragt.filter { erlaubt.contains(it) }.distinct().sorted()
        if (gueltig.isEmpty()) return AlternativAuswahl.LEER

        val ids = gueltig.toSet()
        val nettoGesamt = (basisNetto ?: BigDecimal.ZERO).add(ausgangsGeschaeftsDokumentService.summeAusgewaehlterAlternativenNetto(json, ids))
        val bruttoGesamt = nettoGesamt.add(nettoGesamt.multiply(mwst ?: BigDecimal("0.19"))).setScale(2, RoundingMode.HALF_UP)
        val jsonAuswahl = runCatching { OBJECT_MAPPER.writeValueAsString(gueltig) }.getOrNull()
        return AlternativAuswahl(ids, bruttoGesamt, jsonAuswahl)
    }

    private fun aggregiereProContainer(typ: FreigabeQuellTyp, mapping: List<Array<Any>>): Map<Long, DokumentFreigabe> {
        if (mapping.isEmpty()) return emptyMap()
        val dokZuContainer = mapping.mapNotNull { row ->
            val dokId = row.getOrNull(0) as? Long
            val containerId = row.getOrNull(1) as? Long
            if (dokId != null && containerId != null) dokId to containerId else null
        }.toMap()
        if (dokZuContainer.isEmpty()) return emptyMap()
        return repository.findByQuelle(typ, dokZuContainer.keys.toList()).fold(mutableMapOf()) { acc, freigabe ->
            freigabe.effektivenStatusSetzen()
            val containerId = dokZuContainer[freigabe.quellDokumentId] ?: return@fold acc
            acc.merge(containerId, freigabe, ::pickRelevant)
            acc
        }
    }

    private fun baseFreigabe(gueltigkeitTage: Int): DokumentFreigabe {
        val now = LocalDateTime.now()
        return DokumentFreigabe().apply {
            uuid = UUID.randomUUID().toString()
            erstelltAm = now
            ablaufDatum = now.plusDays(clampGueltigkeitTage(gueltigkeitTage).toLong())
            status = FreigabeStatus.PENDING
        }
    }

    private fun revokeAltePendingFreigaben(quellTyp: FreigabeQuellTyp, quellDokumentId: Long?) {
        if (quellDokumentId == null) return
        val jetzt = LocalDateTime.now()
        repository.findByQuelle(quellTyp, listOf(quellDokumentId)).forEach { alt ->
            if (alt.status == FreigabeStatus.PENDING) {
                alt.status = FreigabeStatus.REVOKED
                alt.dokumentDatei?.takeIf { it.isNotBlank() }?.let {
                    runCatching { dateiSpeicherService.loescheDokumentPdfByDateiname(it) }
                    alt.dokumentDatei = null
                }
                alt.ablaufDatum = jetzt
                repository.save(alt)
            }
        }
    }

    private fun berechneHashOriginal(f: DokumentFreigabe): String =
        sha256Hex(
            listOf(
                f.quellTyp?.name.orEmpty(),
                f.quellDokumentId.toString(),
                f.dokumentNummer.orEmpty(),
                f.dokumentArt.orEmpty(),
                f.dokumentBetrag?.toPlainString().orEmpty(),
                f.kundeEmail.orEmpty(),
                f.positionenSnapshot.orEmpty(),
                hashSalt,
            ).joinToString("|"),
        )

    private fun berechneHashAcceptance(f: DokumentFreigabe, ip: String?, email: String?, zeitpunkt: LocalDateTime): String =
        sha256Hex(
            listOf(
                f.hashOriginal.orEmpty(),
                f.uuid.orEmpty(),
                ip.orEmpty(),
                email.orEmpty(),
                zeitpunkt.toString(),
                f.unterzeichnerName.orEmpty(),
                f.akzeptierteAlternativen.orEmpty(),
                f.akzeptierterBetrag?.toPlainString().orEmpty(),
                hashSalt,
            ).joinToString("|"),
        )

    private fun DokumentFreigabe.effektivenStatusSetzen() {
        if (status == FreigabeStatus.PENDING && istAbgelaufen()) status = FreigabeStatus.EXPIRED
    }

    private data class AlternativAuswahl(
        val idSet: Set<String>,
        val betragBrutto: BigDecimal?,
        val json: String?,
    ) {
        companion object {
            val LEER = AlternativAuswahl(emptySet(), null, null)
        }
    }

    data class FreigabePositionsAnsicht(
        val positionen: List<FreigabePositionDto>,
        val basisNetto: BigDecimal?,
        val basisBrutto: BigDecimal?,
        val mwstProzent: BigDecimal?,
        val hatAlternativen: Boolean,
        val alternativAuswahlBetragBrutto: BigDecimal?,
        val alternativAuswahlJson: String?,
    ) {
        fun positionen(): List<FreigabePositionDto> = positionen
        fun basisNetto(): BigDecimal? = basisNetto
        fun basisBrutto(): BigDecimal? = basisBrutto
        fun mwstProzent(): BigDecimal? = mwstProzent
        fun hatAlternativen(): Boolean = hatAlternativen
        fun alternativAuswahlBetragBrutto(): BigDecimal? = alternativAuswahlBetragBrutto
        fun alternativAuswahlJson(): String? = alternativAuswahlJson
    }

    companion object {
        private val log = LoggerFactory.getLogger(DokumentFreigabeService::class.java)
        private val OBJECT_MAPPER = ObjectMapper()
        private val ABLAUF_DATUM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        const val DEFAULT_GUELTIGKEITS_TAGE = 14
        const val UNBEKANNTE_UUID_MESSAGE = "Unbekannte Freigabe-UUID"
        private const val MIN_GUELTIGKEITS_TAGE = 1
        private const val MAX_GUELTIGKEITS_TAGE = 365

        @JvmStatic
        fun buildFreigabeBlockHtml(url: String, dokumentArt: String?, gueltigkeitTage: Int, ablaufDatum: LocalDateTime?): String {
            val art = dokumentArt?.takeIf { it.isNotBlank() } ?: "Dokument"
            val tageText = if (gueltigkeitTage == 1) "1 Tag" else "$gueltigkeitTage Tage"
            val ablaufText = ablaufDatum?.let { "Der Link ist $tageText gueltig (bis zum ${it.format(ABLAUF_DATUM_FORMAT)})." }
                ?: "Der Link ist $tageText gueltig."
            return "<div style=\"margin:24px 0;padding:16px 18px;border-left:3px solid #500010;background:#fafafa;font-family:Arial,Helvetica,sans-serif;\">" +
                "<p style=\"margin:0 0 6px 0;font-weight:600;color:#1e293b;\">$art digital pruefen und annehmen</p>" +
                "<p style=\"margin:0 0 10px 0;color:#475569;line-height:1.45;\">Sie koennen dieses $art bequem online ansehen und mit einem Klick verbindlich annehmen:</p>" +
                "<p style=\"margin:0;\"><a href=\"$url\" style=\"color:#500010;font-weight:600;text-decoration:underline;\">$url</a></p>" +
                "<p style=\"margin:8px 0 0 0;color:#94a3b8;font-size:13px;\">$ablaufText</p>" +
                "</div>"
        }

        private fun clampGueltigkeitTage(tage: Int): Int =
            if (tage in MIN_GUELTIGKEITS_TAGE..MAX_GUELTIGKEITS_TAGE) tage else DEFAULT_GUELTIGKEITS_TAGE

        private fun istAngebotOderABTyp(typ: AusgangsGeschaeftsDokumentTyp?): Boolean =
            typ == AusgangsGeschaeftsDokumentTyp.ANGEBOT || typ == AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT

        private fun typZuBezeichnung(typ: AusgangsGeschaeftsDokumentTyp?): String =
            when (typ) {
                AusgangsGeschaeftsDokumentTyp.ANGEBOT -> "Angebot"
                AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT -> "Nachtragsangebot"
                AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG -> "Auftragsbestaetigung"
                null -> "Dokument"
                else -> typ.name
            }

        private fun istAngebotOderAB(art: String?): Boolean {
            val lower = art?.lowercase(Locale.GERMAN) ?: return false
            return lower.contains("angebot") || lower.contains("auftragsbest")
        }

        private fun mergeFreigabenProContainer(
            a: Map<Long, DokumentFreigabe>,
            b: Map<Long, DokumentFreigabe>,
        ): Map<Long, DokumentFreigabe> {
            val result = a.toMutableMap()
            b.forEach { (containerId, freigabe) -> result.merge(containerId, freigabe, ::pickRelevant) }
            return result
        }

        private fun pickRelevant(a: DokumentFreigabe, b: DokumentFreigabe): DokumentFreigabe {
            if (a.status == FreigabeStatus.ACCEPTED) return a
            if (b.status == FreigabeStatus.ACCEPTED) return b
            if (a.status == FreigabeStatus.PENDING && b.status != FreigabeStatus.PENDING) return a
            if (b.status == FreigabeStatus.PENDING && a.status != FreigabeStatus.PENDING) return b
            val aZeit = a.erstelltAm ?: LocalDateTime.MIN
            val bZeit = b.erstelltAm ?: LocalDateTime.MIN
            return if (aZeit.isAfter(bZeit)) a else b
        }

        private fun normalisiereName(input: String?): String? =
            input?.replace(Regex("[\\p{Cntrl}]"), " ")
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        private fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return HexFormat.of().formatHex(digest.digest(input.toByteArray(StandardCharsets.UTF_8)))
        }
    }
}
