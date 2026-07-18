package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentCounter
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import org.example.kalkulationsprogramm.domain.Kunde
import org.example.kalkulationsprogramm.domain.Projekt
import org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument.AbrechnungsverlaufDto
import org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument.AusgangsGeschaeftsDokumentErstellenDto
import org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument.AusgangsGeschaeftsDokumentResponseDto
import org.example.kalkulationsprogramm.dto.AusgangsGeschaeftsDokument.AusgangsGeschaeftsDokumentUpdateDto
import org.example.kalkulationsprogramm.dto.Freigabe.FreigabePositionDto
import org.example.kalkulationsprogramm.dto.Produktkategroie.KategorieVorschlagDto
import org.example.kalkulationsprogramm.repository.AnfrageRepository
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentCounterRepository
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository
import org.example.kalkulationsprogramm.repository.FrontendUserProfileRepository
import org.example.kalkulationsprogramm.repository.KundeRepository
import org.example.kalkulationsprogramm.repository.LeistungRepository
import org.example.kalkulationsprogramm.repository.ProduktkategorieRepository
import org.example.kalkulationsprogramm.repository.ProjektDokumentRepository
import org.example.kalkulationsprogramm.repository.ProjektRepository
import org.example.kalkulationsprogramm.repository.ZeitbuchungRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.EnumSet
import java.util.UUID

@Service
open class AusgangsGeschaeftsDokumentService(
    @Value("\${file.upload-dir}") uploadDir: String,
    private val dokumentRepository: AusgangsGeschaeftsDokumentRepository,
    private val counterRepository: AusgangsGeschaeftsDokumentCounterRepository,
    private val projektRepository: ProjektRepository,
    private val anfrageRepository: AnfrageRepository,
    private val kundeRepository: KundeRepository,
    private val frontendUserProfileRepository: FrontendUserProfileRepository,
    private val leistungRepository: LeistungRepository,
    private val produktkategorieRepository: ProduktkategorieRepository,
    private val projektDokumentRepository: ProjektDokumentRepository,
    private val zeitbuchungRepository: ZeitbuchungRepository,
    private val auditService: AusgangsGeschaeftsDokumentAuditService,
) {
    private val dokumentenSpeicherplatz: Path = Path.of(uploadDir).toAbsolutePath().normalize()

    @Transactional
    open fun erstellen(dto: AusgangsGeschaeftsDokumentErstellenDto): AusgangsGeschaeftsDokument = erstellen(dto, null)

    @Transactional
    open fun erstellen(dto: AusgangsGeschaeftsDokumentErstellenDto, ipAdresse: String?): AusgangsGeschaeftsDokument {
        if (dto.typ == null) throw IllegalArgumentException("Dokumenttyp ist erforderlich")
        if (dto.vorgaengerId == null) validiereBasisdokument(dto.typ, dto.projektId, dto.anfrageId)

        val dokument = AusgangsGeschaeftsDokument().apply {
            typ = dto.typ
            datum = dto.datum ?: LocalDate.now()
            betreff = dto.betreff
            betragNetto = dto.betragNetto
            mwstSatz = dto.mwstSatz ?: BigDecimal("0.19")
            zahlungszielTage = dto.zahlungszielTage
            htmlInhalt = dto.htmlInhalt
            positionenJson = dto.positionenJson
            rechnungsadresseOverride = dto.rechnungsadresseOverride
        }

        dto.projektId?.let { id ->
            projektRepository.findById(id).orElse(null)?.let {
                dokument.projekt = it
                if (dto.kundeId == null) dokument.kunde = it.kundenId
            }
        }
        dto.anfrageId?.let { id ->
            anfrageRepository.findById(id).orElse(null)?.let {
                dokument.anfrage = it
                if (dto.kundeId == null) dokument.kunde = it.kunde
                if (dokument.projekt == null) dokument.projekt = it.projekt
            }
        }
        dto.kundeId?.let { dokument.kunde = kundeRepository.findById(it).orElse(null) }
        dto.erstelltVonId?.let { dokument.erstelltVon = frontendUserProfileRepository.findById(it).orElse(null) }

        dto.vorgaengerId?.let { vorgaengerId ->
            val vorgaenger = dokumentRepository.findById(vorgaengerId).orElse(null)
            dokument.vorgaenger = vorgaenger
            if (vorgaenger != null) {
                if (dokument.htmlInhalt == null) dokument.htmlInhalt = vorgaenger.htmlInhalt
                if (dokument.positionenJson == null) dokument.positionenJson = vorgaenger.positionenJson
                if (vorgaenger.typ != dokument.typ && dokument.positionenJson != null) {
                    dokument.positionenJson = entferneStandardTextbausteine(dokument.positionenJson)
                }
                if (dokument.kunde == null) dokument.kunde = vorgaenger.kunde
                if (dokument.anfrage == null) dokument.anfrage = vorgaenger.anfrage
                if (dokument.projekt == null) dokument.projekt = vorgaenger.projekt
                if (RECHNUNGSTYPEN.contains(dto.typ)) {
                    val pruefBetrag = dto.betragNetto ?: dto.positionenJson?.let(::berechneNettoAusPositionenJson)
                    if (pruefBetrag != null) validateRechnungsbetrag(vorgaenger.id, pruefBetrag)
                }
                if (dto.typ == AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG) {
                    dokument.abschlagsNummer =
                        dokumentRepository.countByVorgaengerIdAndTyp(vorgaenger.id, AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG) + 1
                }
            }
        }

        if (dokument.betragNetto == null && !dokument.positionenJson.isNullOrBlank()) {
            dokument.betragNetto = berechneNettoAusPositionenJson(dokument.positionenJson!!)
        }
        dokument.betragBrutto = berechneBrutto(dokument.betragNetto, dokument.mwstSatz)
        dokument.dokumentNummer = generiereNummer(dokument.typ!!)

        val saved = dokumentRepository.save(dokument)
        if (AUDIT_RELEVANTE_TYPEN.contains(saved.typ)) {
            auditService.protokolliereErstellung(saved, saved.erstelltVon, ipAdresse)
        }
        return saved
    }

    @Transactional(readOnly = true)
    open fun findGeerbteRechnungsadresse(projektId: Long?, anfrageId: Long?): String? {
        if (projektId != null) {
            dokumentRepository.findRechnungsadresseOverridesByProjektId(projektId).firstOrNull()?.let { return it }
            projektRepository.findById(projektId).orElse(null)?.kundenId?.let { return buildRechnungsadresse(it) }
        }
        return findGeerbteRechnungsadresseFuerAnfrage(anfrageId)
    }

    @Transactional(readOnly = true)
    open fun findGeerbteRechnungsadresseFuerAnfrage(anfrageId: Long?): String? {
        if (anfrageId == null) return null
        dokumentRepository.findRechnungsadresseOverridesByAnfrageId(anfrageId).firstOrNull()?.let { return it }
        return anfrageRepository.findById(anfrageId).orElse(null)?.kunde?.let { buildRechnungsadresse(it) }
    }

    @Transactional
    open fun ensureAnfrageDokument(anfrageId: Long): String {
        dokumentRepository.findFirstByAnfrageIdAndTyp(anfrageId, AusgangsGeschaeftsDokumentTyp.ANGEBOT)
            .orElse(null)?.dokumentNummer?.let { return it }
        val anfrage = anfrageRepository.findById(anfrageId).orElseThrow { IllegalArgumentException("Anfrage nicht gefunden: $anfrageId") }
        val dto = AusgangsGeschaeftsDokumentErstellenDto(
            typ = AusgangsGeschaeftsDokumentTyp.ANGEBOT,
            anfrageId = anfrageId,
            kundeId = anfrage.kunde?.id,
            betreff = anfrage.bauvorhaben,
        )
        return erstellen(dto).dokumentNummer.orEmpty()
    }

    open fun resolveAnfragesnummer(anfrageId: Long?): String? =
        anfrageId?.let { dokumentRepository.findFirstByAnfrageIdAndTyp(it, AusgangsGeschaeftsDokumentTyp.ANGEBOT).orElse(null)?.dokumentNummer }

    open fun resolveAngebotsnummer(angebotId: Long): String =
        dokumentRepository.findById(angebotId).orElse(null)?.dokumentNummer.orEmpty()

    @Transactional
    open fun aktualisiereAngebotPreisAusDokumenten(angebotId: Long) {
        val angebot = dokumentRepository.findById(angebotId).orElse(null) ?: return
        angebot.betragNetto = angebot.positionenJson?.let(::berechneNettoAusPositionenJson) ?: angebot.betragNetto
        angebot.betragBrutto = berechneBrutto(angebot.betragNetto, angebot.mwstSatz)
        dokumentRepository.save(angebot)
    }

    @Transactional
    open fun aktualisieren(id: Long, dto: AusgangsGeschaeftsDokumentUpdateDto): AusgangsGeschaeftsDokument {
        val dokument = dokumentRepository.findById(id).orElseThrow { IllegalArgumentException("Dokument nicht gefunden: $id") }
        if (!dokument.istBearbeitbar()) throw IllegalStateException("Dokument ist nicht mehr bearbeitbar")
        dto.datum?.let { dokument.datum = it }
        dokument.betreff = dto.betreff
        dokument.betragNetto = dto.betragNetto ?: dto.positionenJson?.let(::berechneNettoAusPositionenJson)
        dokument.mwstSatz = dto.mwstSatz ?: dokument.mwstSatz ?: BigDecimal("0.19")
        dokument.zahlungszielTage = dto.zahlungszielTage
        dokument.htmlInhalt = dto.htmlInhalt
        dokument.positionenJson = dto.positionenJson
        dokument.rechnungsadresseOverride = dto.rechnungsadresseOverride
        dokument.betragBrutto = berechneBrutto(dokument.betragNetto, dokument.mwstSatz)
        val saved = dokumentRepository.save(dokument)
        if (AUDIT_RELEVANTE_TYPEN.contains(saved.typ)) auditService.protokolliereAenderung(saved, saved.erstelltVon, "Dokument aktualisiert", null)
        return saved
    }

    @Transactional
    open fun buchen(id: Long): AusgangsGeschaeftsDokument = buchen(id, null, null)

    @Transactional
    open fun buchen(id: Long, bearbeiterId: Long?, ipAdresse: String?): AusgangsGeschaeftsDokument {
        val dokument = dokumentRepository.findById(id).orElseThrow { IllegalArgumentException("Dokument nicht gefunden: $id") }
        if (NICHT_BUCHBARE_TYPEN.contains(dokument.typ)) throw IllegalStateException("Dokumenttyp kann nicht gebucht werden")
        if (!dokument.gebucht) {
            dokument.gebucht = true
            dokument.gebuchtAm = LocalDate.now()
            val bearbeiter = bearbeiterId?.let { frontendUserProfileRepository.findById(it).orElse(null) }
            if (AUDIT_RELEVANTE_TYPEN.contains(dokument.typ)) auditService.protokolliereBuchung(dokument, bearbeiter, ipAdresse)
        }
        return dokumentRepository.save(dokument)
    }

    @Transactional
    open fun buchenNachEmailVersand(id: Long): AusgangsGeschaeftsDokument = buchenNachEmailVersand(id, null, null)

    @Transactional
    open fun buchenNachEmailVersand(id: Long, bearbeiterId: Long?, ipAdresse: String?): AusgangsGeschaeftsDokument {
        val dokument = buchen(id, bearbeiterId, ipAdresse)
        dokument.versandDatum = LocalDate.now()
        if (AUDIT_RELEVANTE_TYPEN.contains(dokument.typ)) {
            val bearbeiter = bearbeiterId?.let { frontendUserProfileRepository.findById(it).orElse(null) }
            auditService.protokolliereVersand(dokument, bearbeiter, ipAdresse)
        }
        return dokumentRepository.save(dokument)
    }

    @Transactional
    open fun stornieren(id: Long): AusgangsGeschaeftsDokument = stornieren(id, null, null, null)

    @Transactional
    open fun stornieren(id: Long, bearbeiterId: Long?, ipAdresse: String?, grund: String?): AusgangsGeschaeftsDokument {
        val dokument = dokumentRepository.findById(id).orElseThrow { IllegalArgumentException("Dokument nicht gefunden: $id") }
        if (!dokument.storniert) {
            dokument.storniert = true
            dokument.storniertAm = LocalDate.now()
            if (AUDIT_RELEVANTE_TYPEN.contains(dokument.typ)) {
                val bearbeiter = bearbeiterId?.let { frontendUserProfileRepository.findById(it).orElse(null) }
                auditService.protokolliereStornierung(dokument, bearbeiter, grund ?: "Storniert", ipAdresse)
            }
        }
        return dokumentRepository.save(dokument)
    }

    @Transactional
    open fun speicherePdfFuerDokument(dokumentId: Long, pdfBytes: ByteArray): String {
        val dokument = dokumentRepository.findById(dokumentId).orElseThrow { IllegalArgumentException("Dokument nicht gefunden: $dokumentId") }
        val nummer = dokument.dokumentNummer?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "dokument-$dokumentId"
        val filename = "${nummer}_${UUID.randomUUID()}.pdf"
        val dir = dokumentenSpeicherplatz.resolve("ausgangsdokumente").normalize()
        Files.createDirectories(dir)
        Files.write(dir.resolve(filename), pdfBytes)
        return filename
    }

    @Transactional(readOnly = true)
    open fun findByProjekt(projektId: Long): List<AusgangsGeschaeftsDokumentResponseDto> =
        dokumentRepository.findByProjektIdOrderByDatumDesc(projektId).map(::toResponseDto)

    @Transactional(readOnly = true)
    open fun findByAnfrage(anfrageId: Long): List<AusgangsGeschaeftsDokumentResponseDto> =
        dokumentRepository.findByAnfrageIdOrderByDatumDesc(anfrageId).map(::toResponseDto)

    @Transactional
    open fun migrateFromAnfrageToProjekt(anfrageId: Long, projekt: Projekt) {
        dokumentRepository.findByAnfrageIdOrderByDatumDesc(anfrageId).forEach {
            it.projekt = projekt
            it.anfrage = null
            if (it.kunde == null) it.kunde = projekt.kundenId
            dokumentRepository.save(it)
        }
    }

    @Transactional(readOnly = true)
    open fun findById(id: Long): AusgangsGeschaeftsDokumentResponseDto? =
        dokumentRepository.findById(id).map(::toResponseDto).orElse(null)

    @Transactional
    open fun loeschen(id: Long, begruendung: String?) = loeschen(id, begruendung, null, null)

    @Transactional
    open fun loeschen(id: Long, begruendung: String?, geloeschtVonId: Long?, ipAdresse: String?) {
        val dokument = dokumentRepository.findById(id).orElseThrow { IllegalArgumentException("Dokument nicht gefunden: $id") }
        if (dokument.gebucht || AUDIT_RELEVANTE_TYPEN.contains(dokument.typ)) {
            stornieren(id, geloeschtVonId, ipAdresse, begruendung)
        } else {
            dokumentRepository.delete(dokument)
        }
    }

    @Transactional(readOnly = true)
    open fun getAbrechnungsverlauf(basisdokumentId: Long): AbrechnungsverlaufDto {
        val basis = dokumentRepository.findById(basisdokumentId).orElseThrow { IllegalArgumentException("Dokument nicht gefunden: $basisdokumentId") }
        val folge = dokumentRepository.findByVorgaengerIdOrderByErstelltAmAsc(basisdokumentId)
            .filter { RECHNUNGSTYPEN.contains(it.typ) }
        val positionen = folge.map {
            AbrechnungsverlaufDto.AbrechnungspositionDto(
                id = it.id,
                dokumentNummer = it.dokumentNummer,
                typ = it.typ,
                datum = it.datum,
                erstelltAm = it.erstelltAm,
                betragNetto = it.betragNetto,
                abschlagsNummer = it.abschlagsNummer,
                isStorniert = it.storniert,
            )
        }
        val abgerechnet = folge.filterNot { it.storniert }.fold(BigDecimal.ZERO) { sum, d -> sum + (d.betragNetto ?: BigDecimal.ZERO) }
        val basisNetto = basis.betragNetto ?: BigDecimal.ZERO
        return AbrechnungsverlaufDto(
            basisdokumentId = basis.id,
            basisdokumentNummer = basis.dokumentNummer,
            basisdokumentTyp = basis.typ,
            basisdokumentDatum = basis.datum,
            basisdokumentBetragNetto = basis.betragNetto,
            positionen = positionen,
            bereitsAbgerechnet = abgerechnet,
            restbetrag = basisNetto.subtract(abgerechnet).max(BigDecimal.ZERO),
            bereitsAbgerechneteBlockIds = folge.flatMap { extractAbgerechneteBlockIds(it.positionenJson).toList() }.toSet(),
        )
    }

    @Transactional
    open fun aktualisiereProjektPreisAusDokumenten(projektId: Long) {
        val projekt = projektRepository.findById(projektId).orElse(null) ?: return
        val aktive = dokumentRepository.findByProjektIdOrderByDatumDesc(projektId).filterNot { it.storniert }
        projekt.bruttoPreis = berechneAktuellenBruttoPreis(aktive)
        projektRepository.save(projekt)
    }

    @Transactional
    open fun aktualisiereAnfragePreisAusDokumenten(anfrageId: Long?) {
        if (anfrageId == null) return
        val anfrage = anfrageRepository.findById(anfrageId).orElse(null) ?: return
        val aktive = dokumentRepository.findByAnfrageIdOrderByDatumDesc(anfrageId).filterNot { it.storniert }
        anfrage.betrag = berechneAktuellenBruttoPreis(aktive)
        anfrageRepository.save(anfrage)
    }

    @Transactional
    open fun aktualisiereProjektProduktkategorienAusDokumenten(projektId: Long) {
        // Full category synchronization depends on legacy mapping rules. Keep this non-destructive.
        projektRepository.findById(projektId).orElse(null) ?: return
    }

    @Transactional(readOnly = true)
    open fun berechneKategorieVorschlagFuerAnfrage(anfrageId: Long): List<KategorieVorschlagDto> {
        val leistungIds = dokumentRepository.findByAnfrageIdOrderByDatumDesc(anfrageId)
            .filter { KATEGORIE_RELEVANTE_TYPEN.contains(it.typ) }
            .flatMap { extractLeistungIdsFromPositionenJson(it.positionenJson).toList() }
            .distinct()
        if (leistungIds.isEmpty()) return emptyList()
        return leistungRepository.findAllById(leistungIds).mapNotNull { leistung ->
            val kategorie = leistung.kategorie ?: return@mapNotNull null
            KategorieVorschlagDto(
                kategorieId = kategorie.id,
                bezeichnung = kategorie.bezeichnung,
                pfad = bauePfad(kategorie),
                verrechnungseinheit = kategorie.verrechnungseinheit,
                menge = BigDecimal.ONE,
                quelle = "Ausgangsdokument",
            )
        }
    }

    open fun baueKundenPositionen(positionenJson: String?): List<FreigabePositionDto> =
        leseBlocks(positionenJson).mapNotNull(::mapBlockZuPositionDto)

    open fun sammleOptionaleAlternativIds(positionenJson: String?): Set<String> {
        val result = linkedSetOf<String>()
        leseBlocks(positionenJson).forEach { sammleOptionaleAlternativIds(it, result) }
        return result
    }

    open fun summeAusgewaehlterAlternativenNetto(positionenJson: String?, blockIds: Set<String>?): BigDecimal {
        if (blockIds.isNullOrEmpty()) return BigDecimal.ZERO
        return leseBlocks(positionenJson).fold(BigDecimal.ZERO) { sum, block -> sum + summeAusgewaehlt(block, blockIds) }
    }

    open fun markiereAlternativenAlsBeauftragt(positionenJson: String?, blockIds: Set<String>?): String {
        if (positionenJson.isNullOrBlank() || blockIds.isNullOrEmpty()) return positionenJson.orEmpty()
        val root = MAPPER.readTree(positionenJson)
        val blocks = if (root.isArray) root else root.path("blocks")
        if (blocks is ArrayNode) blocks.forEach { markiereBlock(it, blockIds) }
        return MAPPER.writeValueAsString(root)
    }

    open fun bereitePositionenFuerTypwechsel(positionenJson: String?): String =
        entferneStandardTextbausteine(positionenJson)

    private fun validateRechnungsbetrag(vorgaengerId: Long?, neuerBetrag: BigDecimal) {
        if (vorgaengerId == null) return
        val verlauf = getAbrechnungsverlauf(vorgaengerId)
        val rest = verlauf.restbetrag ?: return
        if (neuerBetrag > rest.add(BigDecimal("0.01"))) {
            throw IllegalArgumentException("Rechnungsbetrag uebersteigt Restbetrag")
        }
    }

    private fun berechneNettoAusPositionenJson(positionenJson: String): BigDecimal =
        leseBlocks(positionenJson).fold(BigDecimal.ZERO) { sum, block -> sum + summeServiceBlock(block) }.setScale(2, RoundingMode.HALF_UP)

    private fun summeServiceBlock(block: JsonNode): BigDecimal {
        val typ = textOrNull(block, "typ") ?: textOrNull(block, "type")
        val optional = block.path("optional").asBoolean(false)
        val children = block.path("children")
        var sum = BigDecimal.ZERO
        if ((typ.equals("SERVICE", true) || typ.equals("POSITION", true)) && !optional) {
            sum += zeilenSummeNetto(decimalOrNull(block, "menge"), decimalOrNull(block, "einzelpreisNetto"), decimalOrNull(block, "rabattProzent"))
        }
        if (children.isArray) children.forEach { sum += summeServiceBlock(it) }
        return sum
    }

    private fun entferneStandardTextbausteine(positionenJson: String?): String {
        if (positionenJson.isNullOrBlank()) return positionenJson.orEmpty()
        return runCatching {
            val root = MAPPER.readTree(positionenJson)
            val blocks = if (root.isArray) root as ArrayNode else root.path("blocks") as? ArrayNode
            if (blocks != null) {
                val kept = MAPPER.createArrayNode()
                blocks.filterNot(::istStandardTextbaustein).forEach { kept.add(it) }
                if (root is ArrayNode) {
                    MAPPER.writeValueAsString(kept)
                } else {
                    (root as ObjectNode).set<ArrayNode>("blocks", kept)
                    root.put("standardTextbausteineErneuern", true)
                    MAPPER.writeValueAsString(root)
                }
            } else {
                positionenJson
            }
        }.getOrElse { positionenJson }
    }

    private fun istStandardTextbaustein(block: JsonNode): Boolean {
        val typ = textOrNull(block, "typ") ?: textOrNull(block, "type")
        val role = textOrNull(block, "role") ?: textOrNull(block, "rolle")
        return typ.equals("TEXT", true) && (role.equals("VOR", true) || role.equals("NACH", true) || block.path("standardTextbaustein").asBoolean(false))
    }

    private fun extractAbgerechneteBlockIds(positionenJson: String?): Set<String> {
        val ids = linkedSetOf<String>()
        leseBlocks(positionenJson).forEach { collectAbgerechneteServiceIds(it, ids) }
        return ids
    }

    private fun collectAbgerechneteServiceIds(block: JsonNode, ids: MutableSet<String>) {
        if (block.path("abgerechnet").asBoolean(false)) textOrNull(block, "blockId")?.let(ids::add)
        block.path("children").takeIf { it.isArray }?.forEach { collectAbgerechneteServiceIds(it, ids) }
    }

    private fun validiereBasisdokument(typ: AusgangsGeschaeftsDokumentTyp?, projektId: Long?, anfrageId: Long?) {
        if (typ == AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT) return
        if (projektId != null && dokumentRepository.existsByProjektIdAndVorgaengerIsNull(projektId)) {
            throw IllegalStateException("Es existiert bereits ein Basisdokument fuer dieses Projekt")
        }
        if (anfrageId != null && dokumentRepository.existsByAnfrageIdAndVorgaengerIsNull(anfrageId)) {
            throw IllegalStateException("Es existiert bereits ein Basisdokument fuer diese Anfrage")
        }
    }

    private fun generiereNummer(typ: AusgangsGeschaeftsDokumentTyp): String {
        val ym = YearMonth.now()
        val monatKey = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val counter = counterRepository.findByMonatKeyForUpdate(monatKey).orElseGet {
            counterRepository.save(AusgangsGeschaeftsDokumentCounter().apply {
                this.monatKey = monatKey
                zaehler = 0
            })
        }
        counter.zaehler += 1
        counterRepository.save(counter)
        return "${praefixFuer(typ)}-${ym.format(DateTimeFormatter.ofPattern("yyyyMM"))}-${counter.zaehler.toString().padStart(4, '0')}"
    }

    private fun praefixFuer(typ: AusgangsGeschaeftsDokumentTyp): String =
        when (typ) {
            AusgangsGeschaeftsDokumentTyp.ANGEBOT -> "AN"
            AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT -> "NA"
            AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG -> "AB"
            AusgangsGeschaeftsDokumentTyp.RECHNUNG -> "RE"
            AusgangsGeschaeftsDokumentTyp.TEILRECHNUNG -> "TR"
            AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG -> "AR"
            AusgangsGeschaeftsDokumentTyp.SCHLUSSRECHNUNG -> "SR"
            AusgangsGeschaeftsDokumentTyp.GUTSCHRIFT -> "GS"
            AusgangsGeschaeftsDokumentTyp.STORNO -> "ST"
            AusgangsGeschaeftsDokumentTyp.ZAHLUNGSERINNERUNG -> "ZE"
            AusgangsGeschaeftsDokumentTyp.ERSTE_MAHNUNG -> "M1"
            AusgangsGeschaeftsDokumentTyp.ZWEITE_MAHNUNG -> "M2"
        }

    private fun toResponseDto(dokument: AusgangsGeschaeftsDokument): AusgangsGeschaeftsDokumentResponseDto =
        AusgangsGeschaeftsDokumentResponseDto(
            id = dokument.id,
            dokumentNummer = dokument.dokumentNummer,
            typ = dokument.typ,
            datum = dokument.datum,
            betreff = dokument.betreff,
            htmlInhalt = dokument.htmlInhalt,
            positionenJson = dokument.positionenJson,
            betragNetto = dokument.betragNetto,
            betragBrutto = dokument.betragBrutto,
            mwstSatz = dokument.mwstSatz,
            mwstBetrag = dokument.getMwstBetrag(),
            abschlagsNummer = dokument.abschlagsNummer,
            zahlungszielTage = dokument.zahlungszielTage,
            versandDatum = dokument.versandDatum,
            isGebucht = dokument.gebucht,
            gebuchtAm = dokument.gebuchtAm,
            isStorniert = dokument.storniert,
            storniertAm = dokument.storniertAm,
            isDigitalAngenommen = dokument.digitalAngenommen,
            isBearbeitbar = dokument.istBearbeitbar(),
            projektId = dokument.projekt?.id,
            projektBauvorhaben = dokument.projekt?.bauvorhaben,
            projektnummer = dokument.projekt?.auftragsnummer,
            anfrageId = dokument.anfrage?.id,
            kundeId = dokument.kunde?.id,
            kundennummer = dokument.kunde?.kundennummer,
            kundenName = dokument.kunde?.name,
            rechnungsadresse = dokument.rechnungsadresseOverride ?: dokument.kunde?.let(::buildRechnungsadresse),
            rechnungsadresseOverride = dokument.rechnungsadresseOverride,
            vorgaengerId = dokument.vorgaenger?.id,
            vorgaengerNummer = dokument.vorgaenger?.dokumentNummer,
            erstelltVonId = dokument.erstelltVon?.id,
            erstelltVonName = dokument.erstelltVon?.displayName,
            pdfUrl = dokument.id?.let { "/api/ausgangsdokumente/$it/pdf" },
        )

    private fun buildRechnungsadresse(kunde: Kunde): String =
        listOfNotNull(
            kunde.name,
            kunde.strasse,
            listOfNotNull(kunde.plz, kunde.ort).joinToString(" ").ifBlank { null },
        ).joinToString("\n").ifBlank { null }.orEmpty()

    private fun berechneAktuellenBruttoPreis(aktive: List<AusgangsGeschaeftsDokument>): BigDecimal {
        val kandidat = aktive.firstOrNull { it.typ == AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG }
            ?: aktive.firstOrNull { it.typ == AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT }
            ?: aktive.firstOrNull { it.typ == AusgangsGeschaeftsDokumentTyp.ANGEBOT }
        return kandidat?.betragBrutto ?: BigDecimal.ZERO
    }

    private fun bauePfad(kategorie: org.example.kalkulationsprogramm.domain.Produktkategorie): String =
        generateSequence(kategorie) { it.uebergeordneteKategorie }.toList().asReversed().joinToString(" > ") { it.bezeichnung.orEmpty() }

    private fun extractLeistungIdsFromPositionenJson(positionenJson: String?): Set<Long> {
        val result = linkedSetOf<Long>()
        leseBlocks(positionenJson).forEach { collectLeistungIds(it, result) }
        return result
    }

    private fun collectLeistungIds(block: JsonNode, ids: MutableSet<Long>) {
        val id = block.path("leistungId").takeIf { it.isNumber }?.asLong()
        if (id != null) ids += id
        block.path("children").takeIf { it.isArray }?.forEach { collectLeistungIds(it, ids) }
    }

    private fun mapBlockZuPositionDto(block: JsonNode): FreigabePositionDto? {
        val typ = textOrNull(block, "typ") ?: textOrNull(block, "type")
        if (typ.equals("TEXT", true)) return null
        return FreigabePositionDto.builder()
            .blockId(textOrNull(block, "blockId") ?: textOrNull(block, "id"))
            .typ(typ)
            .pos(textOrNull(block, "pos"))
            .bezeichnung(textOrNull(block, "bezeichnung") ?: textOrNull(block, "title"))
            .beschreibungHtml(textOrNull(block, "beschreibungHtml") ?: textOrNull(block, "html"))
            .menge(decimalOrNull(block, "menge"))
            .einheit(textOrNull(block, "einheit"))
            .einzelpreisNetto(decimalOrNull(block, "einzelpreisNetto"))
            .rabattProzent(decimalOrNull(block, "rabattProzent"))
            .gesamtpreisNetto(decimalOrNull(block, "gesamtpreisNetto") ?: zeilenSummeNetto(decimalOrNull(block, "menge"), decimalOrNull(block, "einzelpreisNetto"), decimalOrNull(block, "rabattProzent")))
            .optional(block.path("optional").asBoolean(false))
            .sectionLabel(textOrNull(block, "sectionLabel"))
            .children(block.path("children").takeIf { it.isArray }?.mapNotNull(::mapBlockZuPositionDto))
            .build()
    }

    private fun sammleOptionaleAlternativIds(block: JsonNode, ids: MutableSet<String>) {
        if (block.path("optional").asBoolean(false)) {
            (textOrNull(block, "blockId") ?: textOrNull(block, "id"))?.let(ids::add)
        }
        block.path("children").takeIf { it.isArray }?.forEach { sammleOptionaleAlternativIds(it, ids) }
    }

    private fun summeAusgewaehlt(block: JsonNode, blockIds: Set<String>): BigDecimal {
        val id = textOrNull(block, "blockId") ?: textOrNull(block, "id")
        var sum = if (id != null && blockIds.contains(id)) {
            zeilenSummeNetto(decimalOrNull(block, "menge"), decimalOrNull(block, "einzelpreisNetto"), decimalOrNull(block, "rabattProzent"))
        } else {
            BigDecimal.ZERO
        }
        block.path("children").takeIf { it.isArray }?.forEach { sum += summeAusgewaehlt(it, blockIds) }
        return sum
    }

    private fun markiereBlock(block: JsonNode, blockIds: Set<String>) {
        if (block is ObjectNode) {
            val id = textOrNull(block, "blockId") ?: textOrNull(block, "id")
            if (id != null && blockIds.contains(id)) {
                block.put("optional", false)
                block.put("beauftragt", true)
            }
        }
        block.path("children").takeIf { it.isArray }?.forEach { markiereBlock(it, blockIds) }
    }

    private fun leseBlocks(positionenJson: String?): List<JsonNode> {
        if (positionenJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = MAPPER.readTree(positionenJson)
            val blocks = if (root.isArray) root else root.path("blocks")
            if (!blocks.isArray) emptyList() else blocks.toList()
        }.getOrElse {
            log.warn("Positionen-JSON konnte nicht gelesen werden: {}", it.message)
            emptyList()
        }
    }

    private fun berechneBrutto(netto: BigDecimal?, mwst: BigDecimal?): BigDecimal? {
        if (netto == null) return null
        val satz = mwst ?: BigDecimal("0.19")
        return netto.add(netto.multiply(satz)).setScale(2, RoundingMode.HALF_UP)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AusgangsGeschaeftsDokumentService::class.java)
        private val MAPPER = ObjectMapper()
        private val RECHNUNGSTYPEN = EnumSet.of(
            AusgangsGeschaeftsDokumentTyp.RECHNUNG,
            AusgangsGeschaeftsDokumentTyp.TEILRECHNUNG,
            AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG,
            AusgangsGeschaeftsDokumentTyp.SCHLUSSRECHNUNG,
        )
        private val AUDIT_RELEVANTE_TYPEN = EnumSet.of(
            AusgangsGeschaeftsDokumentTyp.RECHNUNG,
            AusgangsGeschaeftsDokumentTyp.TEILRECHNUNG,
            AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG,
            AusgangsGeschaeftsDokumentTyp.SCHLUSSRECHNUNG,
            AusgangsGeschaeftsDokumentTyp.GUTSCHRIFT,
            AusgangsGeschaeftsDokumentTyp.STORNO,
        )
        private val KATEGORIE_RELEVANTE_TYPEN = EnumSet.of(
            AusgangsGeschaeftsDokumentTyp.ANGEBOT,
            AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT,
            AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG,
        )
        private val NICHT_BUCHBARE_TYPEN = EnumSet.of(
            AusgangsGeschaeftsDokumentTyp.ANGEBOT,
            AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT,
            AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG,
        )

        private fun textOrNull(node: JsonNode, field: String): String? =
            node.path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }

        private fun decimalOrNull(node: JsonNode, field: String): BigDecimal? =
            node.path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }?.let {
                runCatching { BigDecimal(it) }.getOrNull()
            }

        private fun zeilenSummeNetto(menge: BigDecimal?, preis: BigDecimal?, rabattProzent: BigDecimal?): BigDecimal {
            val basis = (menge ?: BigDecimal.ONE).multiply(preis ?: BigDecimal.ZERO)
            val rabatt = rabattProzent ?: BigDecimal.ZERO
            return if (rabatt.compareTo(BigDecimal.ZERO) == 0) {
                basis
            } else {
                basis.multiply(BigDecimal("100").subtract(rabatt)).divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
            }
        }
    }
}
