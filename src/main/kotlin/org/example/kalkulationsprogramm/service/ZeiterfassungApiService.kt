package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.dto.Arbeitsgang.ArbeitsgangResponseDto
import org.example.kalkulationsprogramm.mapper.ArbeitsgangMapper
import org.example.kalkulationsprogramm.domain.AbwesenheitsTyp
import org.example.kalkulationsprogramm.domain.BuchungsTyp
import org.example.kalkulationsprogramm.domain.DokumentGruppe
import org.example.kalkulationsprogramm.domain.ErfassungsQuelle
import org.example.kalkulationsprogramm.domain.Zeitkonto
import org.example.kalkulationsprogramm.domain.Zeitbuchung
import org.example.kalkulationsprogramm.repository.AbwesenheitRepository
import org.example.kalkulationsprogramm.repository.ArbeitsgangStundensatzRepository
import org.example.kalkulationsprogramm.repository.ArbeitsgangRepository
import org.example.kalkulationsprogramm.repository.FeiertagRepository
import org.example.kalkulationsprogramm.repository.LieferantenRepository
import org.example.kalkulationsprogramm.repository.MitarbeiterRepository
import org.example.kalkulationsprogramm.repository.ProduktkategorieRepository
import org.example.kalkulationsprogramm.repository.ProjektRepository
import org.example.kalkulationsprogramm.repository.ZeitbuchungRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.Optional

@Service
class ZeiterfassungApiService(
    private val projektRepository: ProjektRepository,
    private val produktkategorieRepository: ProduktkategorieRepository,
    private val arbeitsgangRepository: ArbeitsgangRepository,
    private val arbeitsgangMapper: ArbeitsgangMapper,
    private val lieferantenRepository: LieferantenRepository,
    private val feiertagRepository: FeiertagRepository,
    private val mitarbeiterRepository: MitarbeiterRepository,
    private val zeitbuchungRepository: ZeitbuchungRepository,
    private val dateiSpeicherService: DateiSpeicherService,
    private val arbeitsgangStundensatzRepository: ArbeitsgangStundensatzRepository,
    private val auditService: ZeitbuchungAuditService,
    private val monatsSaldoService: MonatsSaldoService,
    private val abwesenheitRepository: AbwesenheitRepository,
    private val zeitkontoService: ZeitkontoService,
    private val urlaubsverfallService: UrlaubsverfallService,
    private val zeitkontoKorrekturService: ZeitkontoKorrekturService,
    private val feiertagService: FeiertagService,
) {
    fun getOpenProjekte(limit: Int?, search: String?): List<Map<String, Any>> {
        val max = (limit ?: 100).coerceIn(1, 1000)
        return projektRepository.findSimpleByQuery(search, org.springframework.data.domain.PageRequest.of(0, max))
            .asSequence()
            .filter { !it.isAbgeschlossen() }
            .map {
                mapOf(
                    "id" to it.getId(),
                    "bauvorhaben" to it.getBauvorhaben(),
                    "auftragsnummer" to it.getAuftragsnummer(),
                    "kunde" to it.getKunde(),
                    "abgeschlossen" to it.isAbgeschlossen(),
                )
            }
            .toList()
    }

    fun getKategorienMitPfad(): List<Map<String, Any>> =
        produktkategorieRepository.findAllWithParent()
            .sortedBy { it.bezeichnung.orEmpty() }
            .map { kategorie ->
                mapOf(
                    "id" to (kategorie.id ?: 0L),
                    "bezeichnung" to kategorie.bezeichnung.orEmpty(),
                    "parentId" to (kategorie.uebergeordneteKategorie?.id ?: ""),
                    "parentBezeichnung" to kategorie.uebergeordneteKategorie?.bezeichnung.orEmpty(),
                    "pfad" to buildKategoriePfad(kategorie),
                    "verrechnungseinheit" to kategorie.verrechnungseinheit?.name.orEmpty(),
                )
            }

    fun getKategorienByProjektId(projektId: Long): List<Map<String, Any>> =
        projektRepository.findById(projektId)
            .map { projekt ->
                projekt.projektProduktkategorien.mapNotNull { ppk ->
                    val kategorie = ppk.produktkategorie ?: return@mapNotNull null
                    mapOf(
                        "id" to (kategorie.id ?: 0L),
                        "projektProduktkategorieId" to (ppk.id ?: 0L),
                        "bezeichnung" to kategorie.bezeichnung.orEmpty(),
                        "pfad" to buildKategoriePfad(kategorie),
                        "menge" to ppk.menge,
                        "verrechnungseinheit" to kategorie.verrechnungseinheit?.name.orEmpty(),
                    )
                }
            }
            .orElse(emptyList())

    fun getArbeitsgaengeByMitarbeiterToken(token: String): Optional<List<ArbeitsgangResponseDto>> =
        mitarbeiterRepository.findByLoginTokenAndAktivTrue(token.trim()).map {
            arbeitsgangRepository.findAll()
                .mapNotNull(arbeitsgangMapper::toArbeitsgangResponseDto)
                .sortedBy { dto -> dto.beschreibung.orEmpty() }
        }

    fun getLieferanten(limit: Int?, search: String?): List<Map<String, Any>> {
        val max = (limit ?: 100).coerceIn(1, 1000)
        val lieferanten = if (search.isNullOrBlank()) {
            lieferantenRepository.findByIstAktivTrueOrderByLieferantennameAsc()
        } else {
            lieferantenRepository.searchByNameOrEmail(search.trim())
        }
        return lieferanten
            .asSequence()
            .take(max)
            .map {
                mapOf(
                    "id" to (it.id ?: 0L),
                    "name" to it.lieferantenname.orEmpty(),
                    "lieferantenname" to it.lieferantenname.orEmpty(),
                    "ort" to it.ort.orEmpty(),
                    "email" to it.kundenEmails.firstOrNull().orEmpty(),
                    "aktiv" to (it.istAktiv != false),
                )
            }
            .toList()
    }

    fun startZeiterfassung(
        token: String,
        projektId: Long,
        arbeitsgangId: Long,
        produktkategorieId: Long?,
        originalStartZeit: LocalDateTime?,
        idempotencyKey: String?,
    ): Map<String, Any> {
        findExistingStart(idempotencyKey, produktkategorieId)?.let { return it }
        val mitarbeiter = mitarbeiterRepository.findByLoginTokenAndAktivTrueForUpdate(token.trim())
            .orElseThrow { RuntimeException("Mitarbeiter nicht gefunden") }
        findExistingStart(idempotencyKey, produktkategorieId)?.let { return it }
        if (zeitbuchungRepository.findByMitarbeiterIdAndEndeZeitIsNull(mitarbeiter.id).isNotEmpty()) {
            throw RuntimeException("Es läuft bereits eine Buchung. Bitte erst stoppen.")
        }
        val projekt = projektRepository.findById(projektId).orElseThrow { RuntimeException("Projekt nicht gefunden") }
        val arbeitsgang = arbeitsgangRepository.findById(arbeitsgangId).orElseThrow { RuntimeException("Arbeitsgang nicht gefunden") }
        val startZeit = originalStartZeit ?: LocalDateTime.now()
        val buchung = Zeitbuchung().apply {
            this.mitarbeiter = mitarbeiter
            this.projekt = projekt
            this.arbeitsgang = arbeitsgang
            this.startZeit = startZeit
            typ = BuchungsTyp.ARBEIT
            erfasstVon = mitarbeiter
            erfasstAm = LocalDateTime.now()
            erfasstVia = ErfassungsQuelle.MOBILE_APP
            version = 1
            if (!idempotencyKey.isNullOrBlank()) this.idempotencyKey = idempotencyKey
            projektProduktkategorie = produktkategorieId?.let { id ->
                projekt.projektProduktkategorien.firstOrNull { it.produktkategorie?.id == id }
            }
            arbeitsgangStundensatz = resolveStundensatz(arbeitsgangId, startZeit.year, arbeitsgang.beschreibung)
        }
        val gespeichert = zeitbuchungRepository.save(buchung)
        auditService.protokolliereErstellung(gespeichert, mitarbeiter, ErfassungsQuelle.MOBILE_APP)
        monatsSaldoService.invalidiereFuerDateTime(mitarbeiter.id!!, gespeichert.startZeit)
        return linkedMapOf<String, Any>().apply {
            gespeichert.id?.let { put("id", it) }
            put("projektId", projektId)
            projekt.bauvorhaben?.let { put("projektName", it) }
            put("arbeitsgangId", arbeitsgangId)
            arbeitsgang.beschreibung?.let { put("arbeitsgangName", it) }
            produktkategorieId?.let { put("produktkategorieId", it) }
            put("startZeit", gespeichert.startZeit.toString())
            put("status", "gestartet")
        }
    }

    fun stopZeiterfassung(token: String, originalEndeZeit: LocalDateTime?, idempotencyKey: String?): Map<String, Any> =
        stopAktiveBuchung(token, originalEndeZeit, idempotencyKey, "Zeiterfassung beendet (Stop-Button am Handy)")

    fun startPause(token: String, originalZeit: LocalDateTime?, idempotencyKey: String?): Map<String, Any> {
        findExistingPause(idempotencyKey)?.let { return it }
        val mitarbeiter = mitarbeiterRepository.findByLoginTokenAndAktivTrueForUpdate(token.trim())
            .orElseThrow { RuntimeException("Mitarbeiter nicht gefunden") }
        findExistingPause(idempotencyKey)?.let { return it }
        val pauseStart = originalZeit ?: LocalDateTime.now()
        zeitbuchungRepository.findByMitarbeiterIdAndEndeZeitIsNull(mitarbeiter.id)
            .sortedBy { it.startZeit }
            .forEach { buchung ->
                closeBuchung(buchung, mitarbeiter, pauseStart, "Beendet beim Anstechen einer Pause am Handy")
                zeitbuchungRepository.saveAndFlush(buchung)
                monatsSaldoService.invalidiereFuerDateTime(mitarbeiter.id!!, buchung.startZeit)
            }
        val pause = Zeitbuchung().apply {
            this.mitarbeiter = mitarbeiter
            projekt = null
            startZeit = pauseStart
            typ = BuchungsTyp.PAUSE
            notiz = "Pausenbuchung"
            erfasstVon = mitarbeiter
            erfasstAm = LocalDateTime.now()
            erfasstVia = ErfassungsQuelle.MOBILE_APP
            version = 1
            if (!idempotencyKey.isNullOrBlank()) this.idempotencyKey = idempotencyKey
        }
        val gespeichert = zeitbuchungRepository.save(pause)
        auditService.protokolliereErstellung(gespeichert, mitarbeiter, ErfassungsQuelle.MOBILE_APP)
        monatsSaldoService.invalidiereFuerDateTime(mitarbeiter.id!!, gespeichert.startZeit)
        return pauseResponse(gespeichert, "gestartet", false)
    }

    fun getAktiveBuchung(token: String): Optional<Map<String, Any>> =
        mitarbeiterRepository.findByLoginTokenAndAktivTrue(token.trim())
            .flatMap { mitarbeiter ->
                zeitbuchungRepository.findFirstByMitarbeiterIdAndEndeZeitIsNullOrderByStartZeitDesc(mitarbeiter.id)
            }
            .map(::aktiveBuchungToMap)

    fun getHeuteGearbeitet(token: String): Map<String, Any> {
        val result = linkedMapOf<String, Any>(
            "stunden" to 0,
            "minuten" to 0,
            "buchungenAnzahl" to 0,
        )
        val mitarbeiter = mitarbeiterRepository.findByLoginTokenAndAktivTrue(token.trim()).orElse(null) ?: return result
        val heute = LocalDate.now().atStartOfDay()
        var totalMinuten = 0L
        var anzahlArbeitsBuchungen = 0
        var aktiveBuchungStartZeit: String? = null
        zeitbuchungRepository.findByMitarbeiterIdAndStartZeitAfter(mitarbeiter.id, heute).forEach { buchung ->
            if (buchung.typ == BuchungsTyp.PAUSE) return@forEach
            val stunden = buchung.anzahlInStunden
            if (stunden != null) {
                anzahlArbeitsBuchungen++
                totalMinuten += stunden.multiply(BigDecimal.valueOf(60)).toLong()
            } else if (buchung.endeZeit == null && buchung.startZeit != null) {
                aktiveBuchungStartZeit = buchung.startZeit.toString()
            }
        }
        result["stunden"] = (totalMinuten / 60).toInt()
        result["minuten"] = (totalMinuten % 60).toInt()
        result["buchungenAnzahl"] = anzahlArbeitsBuchungen
        aktiveBuchungStartZeit?.let { result["aktiveBuchungStartZeit"] = it }
        return result
    }

    fun getBuchungenByDatum(token: String, datum: LocalDate): List<Map<String, Any>> {
        val mitarbeiter = mitarbeiterRepository.findByLoginTokenAndAktivTrue(token.trim()).orElse(null) ?: return emptyList()
        val startOfDay = datum.atStartOfDay()
        val endOfDay = datum.plusDays(1).atStartOfDay()
        return zeitbuchungRepository.findByMitarbeiterIdAndStartZeitBetween(mitarbeiter.id, startOfDay, endOfDay)
            .map(::zeitbuchungToMobileMap)
    }

    fun getProjektBilder(projektId: Long): List<Map<String, Any>> =
        dateiSpeicherService.holeDokumenteZuProjekt(projektId)
            .asSequence()
            .filter { it.dokumentGruppe == DokumentGruppe.BILDER }
            .map { dok ->
                linkedMapOf<String, Any>().apply {
                    dok.id?.let { put("id", it) }
                    dok.originalDateiname?.let { put("name", it) }
                    dok.gespeicherterDateiname?.let {
                        put("url", "/api/dokumente/$it")
                        put("thumbnailUrl", "/api/dokumente/$it/thumbnail")
                    }
                    dok.uploadDatum?.let { put("uploadDatum", it) }
                    dok.uploadedBy?.vorname?.let { put("uploadedByVorname", it) }
                    dok.uploadedBy?.nachname?.let { put("uploadedByNachname", it) }
                }
            }
            .toList()

    fun getFeiertage(jahr: Int): List<Map<String, Any>> =
        feiertagRepository.findByJahr(jahr)
            .sortedBy { it.datum }
            .map {
                mapOf(
                    "id" to (it.id ?: 0L),
                    "datum" to it.datum.toString(),
                    "bezeichnung" to it.bezeichnung.orEmpty(),
                    "bundesland" to it.bundesland.orEmpty(),
                    "halbTag" to it.halbTag,
                )
            }

    fun getSaldo(token: String, jahr: Int?, monat: Int?, gesamtBisHeute: Boolean?): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        val mitarbeiter = mitarbeiterRepository.findByLoginTokenAndAktivTrue(token.trim()).orElse(null)
            ?: return linkedMapOf("error" to "Mitarbeiter nicht gefunden")

        val currentYear = jahr ?: LocalDate.now().year
        val currentMonth = monat ?: LocalDate.now().monthValue
        val heute = LocalDate.now()
        val jahresanfang = LocalDate.of(currentYear, 1, 1)
        val jahresende = LocalDate.of(currentYear, 12, 31)

        val jahresUrlaub = mitarbeiter.jahresUrlaub ?: 30
        val abwesenheitenImJahr = abwesenheitRepository.findByMitarbeiterIdAndDatumBetween(
            mitarbeiter.id,
            jahresanfang,
            jahresende,
        )
        val urlaubstagGenommen = abwesenheitenImJahr.count {
            it.typ == AbwesenheitsTyp.URLAUB && it.datum?.isBefore(heute) == true
        }.toLong()
        val urlaubstagGeplant = abwesenheitenImJahr.count {
            it.typ == AbwesenheitsTyp.URLAUB && it.datum?.isBefore(heute) != true
        }.toLong()
        val krankheitsTage = abwesenheitenImJahr.count { it.typ == AbwesenheitsTyp.KRANKHEIT }.toLong()
        val fortbildungsTage = abwesenheitenImJahr.count { it.typ == AbwesenheitsTyp.FORTBILDUNG }.toLong()
        val manuellKorrekturTage = zeitkontoKorrekturService
            .summiereAktiveUrlaubsKorrekturen(mitarbeiter.id!!, currentYear)
            .toInt()

        result["urlaub"] = linkedMapOf<String, Any>(
            "jahresanspruch" to jahresUrlaub,
            "genommen" to urlaubstagGenommen,
            "geplant" to urlaubstagGeplant,
            "korrektur" to manuellKorrekturTage,
            "verbleibend" to (jahresUrlaub - urlaubstagGenommen - urlaubstagGeplant + manuellKorrekturTage).coerceAtLeast(0),
            "krankheitsTage" to krankheitsTage,
            "fortbildungsTage" to fortbildungsTage,
        )

        val monatsSaldo = monatsSaldoService.getOrBerechne(mitarbeiter.id!!, currentYear, currentMonth)
        val sollStundenMonat = monatsSaldo.sollStunden
        val monatsDifferenz = monatsSaldo.getGesamtIst().subtract(sollStundenMonat)
        result["monat"] = linkedMapOf<String, Any>(
            "name" to Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.GERMAN),
            "monatNummer" to currentMonth,
            "sollStunden" to sollStundenMonat,
            "istStunden" to monatsSaldo.getGesamtIst(),
            "differenz" to monatsDifferenz,
        )

        var startDatum = mitarbeiter.eintrittsdatum
        if (startDatum == null) {
            startDatum = zeitbuchungRepository.findFirstByMitarbeiterIdOrderByStartZeitAsc(mitarbeiter.id)
                .map { it.startZeit?.toLocalDate() ?: LocalDate.of(currentYear, 1, 1) }
                .orElse(LocalDate.of(currentYear, 1, 1))
        }

        val endDatum = when {
            gesamtBisHeute == true -> heute
            currentYear == heute.year -> heute
            else -> LocalDate.of(currentYear, 12, 31)
        }

        var gesamtIst = BigDecimal.ZERO
        var gesamtSoll = BigDecimal.ZERO
        var ym = YearMonth.from(startDatum)
        val endYm = YearMonth.from(endDatum)
        while (!ym.isAfter(endYm)) {
            val ms = monatsSaldoService.getOrBerechne(mitarbeiter.id!!, ym.year, ym.monthValue)
            val istErsterMonat = ym == YearMonth.from(startDatum) && startDatum.dayOfMonth > 1
            val istLetzterMonat = ym == endYm && endDatum.dayOfMonth < ym.lengthOfMonth()
            if (istErsterMonat || istLetzterMonat) {
                val monatVon = if (istErsterMonat) startDatum else ym.atDay(1)
                val monatBis = if (istLetzterMonat) endDatum else ym.atEndOfMonth()
                gesamtIst = gesamtIst.add(berechneAnteiligenMonatIst(mitarbeiter.id!!, monatVon, monatBis))
                gesamtSoll = gesamtSoll.add(
                    zeitkontoService.berechneSollstundenFuerZeitraum(
                        zeitkontoService.getOrCreateZeitkonto(mitarbeiter.id),
                        monatVon,
                        monatBis,
                    )
                )
            } else {
                gesamtIst = gesamtIst.add(ms.getGesamtIst())
                gesamtSoll = gesamtSoll.add(ms.sollStunden)
            }
            ym = ym.plusMonths(1)
        }

        result["gesamt"] = linkedMapOf<String, Any>(
            "istStunden" to gesamtIst,
            "sollStunden" to gesamtSoll,
            "saldo" to gesamtIst.subtract(gesamtSoll),
            "startDatum" to startDatum.toString(),
            "endDatum" to endDatum.toString(),
        )
        result["mitarbeiterName"] = "${mitarbeiter.vorname} ${mitarbeiter.nachname}"
        result["jahr"] = currentYear
        return result
    }

    fun getUrlaubsverfallWarnung(token: String): Map<String, Any> =
        urlaubsverfallService.pruefeVerfallWarnungByToken(token.trim()).orElse(emptyMap())

    fun getBuchungszeitfenster(token: String): Map<String, Any> {
        val mitarbeiter = mitarbeiterRepository.findByLoginTokenAndAktivTrue(token.trim())
            .orElseThrow { RuntimeException("Mitarbeiter nicht gefunden") }
        val konto = zeitkontoService.getOrCreateZeitkonto(mitarbeiter.id)
        return linkedMapOf<String, Any>().apply {
            konto.buchungStartZeit?.let { put("buchungStartZeit", it.toString()) }
            konto.buchungEndeZeit?.let { put("buchungEndeZeit", it.toString()) }
        }
    }

    private fun buildKategoriePfad(kategorie: org.example.kalkulationsprogramm.domain.Produktkategorie): String {
        val parts = ArrayDeque<String>()
        var current: org.example.kalkulationsprogramm.domain.Produktkategorie? = kategorie
        while (current != null) {
            current.bezeichnung?.takeIf { it.isNotBlank() }?.let { parts.addFirst(it) }
            current = current.uebergeordneteKategorie
        }
        return parts.joinToString(" / ")
    }

    private fun zeitbuchungToMobileMap(buchung: Zeitbuchung): Map<String, Any> {
        val entry = linkedMapOf<String, Any>()
        buchung.id?.let { entry["id"] = it }
        buchung.startZeit?.let { entry["startMinuten"] = it.hour * 60 + it.minute }
        buchung.endeZeit?.let { entry["endeMinuten"] = it.hour * 60 + it.minute }
        val stunden = buchung.anzahlInStunden
        when {
            stunden != null -> entry["dauerMinuten"] = stunden.multiply(BigDecimal.valueOf(60)).toInt()
            buchung.startZeit != null && buchung.endeZeit != null ->
                entry["dauerMinuten"] = Duration.between(buchung.startZeit, buchung.endeZeit).toMinutes().toInt()
        }

        val projekt = buchung.projekt
        if (projekt != null) {
            projekt.id?.let { entry["projektId"] = it }
            projekt.auftragsnummer?.let { entry["projektNummer"] = it }
            projekt.bauvorhaben?.let { entry["projektName"] = it }
            projekt.getKunde()?.let { entry["kundenName"] = it }
        } else if (buchung.typ == BuchungsTyp.PAUSE) {
            entry["projektName"] = "Pause"
        }

        buchung.arbeitsgang?.let {
            it.id?.let { id -> entry["arbeitsgangId"] = id }
            it.beschreibung?.let { beschreibung -> entry["taetigkeit"] = beschreibung }
        }
        buchung.projektProduktkategorie?.produktkategorie?.let {
            it.id?.let { id -> entry["kategorieId"] = id }
            entry["kategorieName"] = buildKategoriePfad(it)
        }
        buchung.notiz?.let { entry["kommentar"] = it }
        entry["typ"] = buchung.typ?.name ?: BuchungsTyp.ARBEIT.name
        return entry
    }

    private fun berechneFeiertagsStunden(zeitkonto: Zeitkonto, von: LocalDate, bis: LocalDate): BigDecimal {
        var summe = BigDecimal.ZERO
        var tag = von
        while (!tag.isAfter(bis)) {
            val tagesSoll = zeitkonto.getSollstundenFuerTag(tag.dayOfWeek.value)
            if (tagesSoll > BigDecimal.ZERO && feiertagService.istFeiertag(tag)) {
                summe = summe.add(
                    if (feiertagService.istHalberFeiertag(tag)) {
                        tagesSoll.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                    } else {
                        tagesSoll
                    }
                )
            }
            tag = tag.plusDays(1)
        }
        return summe
    }

    private fun berechneAnteiligenMonatIst(mitarbeiterId: Long, von: LocalDate, bis: LocalDate): BigDecimal {
        val vonDateTime = von.atStartOfDay()
        val bisDateTime = bis.atTime(23, 59, 59)
        val istStunden = zeitbuchungRepository.findByMitarbeiterIdAndStartZeitBetween(mitarbeiterId, vonDateTime, bisDateTime)
            .asSequence()
            .filter { it.typ != BuchungsTyp.PAUSE }
            .mapNotNull { it.anzahlInStunden }
            .fold(BigDecimal.ZERO, BigDecimal::add)
        val abwesenheitsStunden = abwesenheitRepository.sumStundenByMitarbeiterIdAndDatumBetween(mitarbeiterId, von, bis)
        val zeitkonto = zeitkontoService.getOrCreateZeitkonto(mitarbeiterId)
        val feiertagsStunden = berechneFeiertagsStunden(zeitkonto, von, bis)
        val korrekturStunden = zeitkontoKorrekturService.summiereAktiveKorrekturenImZeitraum(mitarbeiterId, von, bis)
        return istStunden.add(abwesenheitsStunden).add(feiertagsStunden).add(korrekturStunden)
    }

    private fun stopAktiveBuchung(
        token: String,
        originalEndeZeit: LocalDateTime?,
        idempotencyKey: String?,
        grund: String,
    ): Map<String, Any> {
        findExistingStop(idempotencyKey)?.let { return it }
        val mitarbeiter = mitarbeiterRepository.findByLoginTokenAndAktivTrueForUpdate(token.trim())
            .orElseThrow { RuntimeException("Mitarbeiter nicht gefunden") }
        findExistingStop(idempotencyKey)?.let { return it }
        val buchung = zeitbuchungRepository.findFirstByMitarbeiterIdAndEndeZeitIsNullOrderByStartZeitDesc(mitarbeiter.id)
            .orElseThrow { RuntimeException("Keine aktive Buchung gefunden") }
        closeBuchung(buchung, mitarbeiter, originalEndeZeit ?: LocalDateTime.now(), grund)
        if (!idempotencyKey.isNullOrBlank()) buchung.stopIdempotencyKey = idempotencyKey
        val gespeichert = zeitbuchungRepository.save(buchung)
        monatsSaldoService.invalidiereFuerDateTime(mitarbeiter.id!!, gespeichert.startZeit)
        return stopResponse(gespeichert, "gestoppt", false)
    }

    private fun closeBuchung(buchung: Zeitbuchung, mitarbeiter: org.example.kalkulationsprogramm.domain.Mitarbeiter, requestedEnde: LocalDateTime, grund: String) {
        val start = buchung.startZeit ?: requestedEnde.minusMinutes(1)
        val ende = if (requestedEnde.isAfter(start)) requestedEnde else start.plusMinutes(1)
        buchung.endeZeit = ende
        buchung.anzahlInStunden = BigDecimal.valueOf(Duration.between(start, ende).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
        buchung.markiereAlsGeaendert(mitarbeiter)
        auditService.protokolliereAenderung(buchung, mitarbeiter, ErfassungsQuelle.MOBILE_APP, grund)
    }

    private fun findExistingStart(idempotencyKey: String?, produktkategorieId: Long?): Map<String, Any>? =
        idempotencyKey?.takeIf { it.isNotBlank() }
            ?.let { zeitbuchungRepository.findByIdempotencyKey(it).orElse(null) }
            ?.let { buildIdempotentStartResponse(it, produktkategorieId) }

    private fun findExistingStop(idempotencyKey: String?): Map<String, Any>? =
        idempotencyKey?.takeIf { it.isNotBlank() }
            ?.let { zeitbuchungRepository.findByStopIdempotencyKey(it).orElse(null) }
            ?.let { stopResponse(it, "already_exists", true) }

    private fun findExistingPause(idempotencyKey: String?): Map<String, Any>? =
        idempotencyKey?.takeIf { it.isNotBlank() }
            ?.let { zeitbuchungRepository.findByIdempotencyKey(it).orElse(null) }
            ?.let { pauseResponse(it, "already_exists", true) }

    private fun buildIdempotentStartResponse(buchung: Zeitbuchung, produktkategorieId: Long?): Map<String, Any> =
        linkedMapOf<String, Any>().apply {
            buchung.id?.let { put("id", it) }
            buchung.projekt?.id?.let { put("projektId", it) }
            buchung.projekt?.bauvorhaben?.let { put("projektName", it) }
            buchung.arbeitsgang?.id?.let { put("arbeitsgangId", it) }
            buchung.arbeitsgang?.beschreibung?.let { put("arbeitsgangName", it) }
            produktkategorieId?.let { put("produktkategorieId", it) }
            buchung.startZeit?.let { put("startZeit", it.toString()) }
            put("status", "already_exists")
            put("idempotent", true)
        }

    private fun stopResponse(buchung: Zeitbuchung, status: String, idempotent: Boolean): Map<String, Any> =
        linkedMapOf<String, Any>().apply {
            buchung.id?.let { put("id", it) }
            buchung.projekt?.bauvorhaben?.let { put("projektName", it) }
            buchung.arbeitsgang?.beschreibung?.let { put("arbeitsgangName", it) }
            buchung.startZeit?.let { put("startZeit", it.toString()) }
            buchung.endeZeit?.let { put("endeZeit", it.toString()) }
            buchung.anzahlInStunden?.let { put("stunden", it) }
            put("typ", buchung.typ?.name ?: BuchungsTyp.ARBEIT.name)
            put("status", status)
            if (idempotent) put("idempotent", true)
        }

    private fun pauseResponse(buchung: Zeitbuchung, status: String, idempotent: Boolean): Map<String, Any> =
        linkedMapOf<String, Any>().apply {
            buchung.id?.let { put("id", it) }
            buchung.startZeit?.let { put("startZeit", it.toString()) }
            put("typ", buchung.typ?.name ?: BuchungsTyp.PAUSE.name)
            put("status", status)
            if (idempotent) put("idempotent", true)
        }

    private fun aktiveBuchungToMap(buchung: Zeitbuchung): Map<String, Any> =
        linkedMapOf<String, Any>().apply {
            buchung.id?.let { put("id", it) }
            val projekt = buchung.projekt
            if (projekt != null) {
                projekt.id?.let { put("projektId", it) }
                projekt.bauvorhaben?.let { put("projektName", it) }
                projekt.getKunde()?.let { put("kundenName", it) }
                projekt.auftragsnummer?.let { put("auftragsnummer", it) }
            } else if (buchung.typ == BuchungsTyp.PAUSE) {
                put("projektName", "Pause")
            }
            buchung.arbeitsgang?.id?.let { put("arbeitsgangId", it) }
            buchung.arbeitsgang?.beschreibung?.let { put("arbeitsgangName", it) }
            buchung.projektProduktkategorie?.produktkategorie?.let {
                it.id?.let { id -> put("produktkategorieId", id) }
                it.bezeichnung?.let { name -> put("produktkategorieName", name) }
            }
            buchung.typ?.name?.let { put("typ", it) }
            buchung.startZeit?.let { put("startZeit", it.toString()) }
        }

    private fun resolveStundensatz(arbeitsgangId: Long, jahr: Int, arbeitsgangName: String?) =
        arbeitsgangStundensatzRepository.findTopByArbeitsgangIdAndJahrOrderByIdDesc(arbeitsgangId, jahr)
            .or { arbeitsgangStundensatzRepository.findTopByArbeitsgangIdAndJahrGreaterThanEqualOrderByJahrAsc(arbeitsgangId, jahr) }
            .or { arbeitsgangStundensatzRepository.findTopByArbeitsgangIdOrderByJahrDesc(arbeitsgangId) }
            .orElseThrow { RuntimeException("Kein Stundensatz für Arbeitsgang '${arbeitsgangName.orEmpty()}' gefunden") }
}
