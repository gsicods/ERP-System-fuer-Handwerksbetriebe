package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.dto.Arbeitsgang.ArbeitsgangResponseDto
import org.example.kalkulationsprogramm.mapper.ArbeitsgangMapper
import org.example.kalkulationsprogramm.domain.BuchungsTyp
import org.example.kalkulationsprogramm.domain.DokumentGruppe
import org.example.kalkulationsprogramm.domain.Zeitbuchung
import org.example.kalkulationsprogramm.repository.ArbeitsgangRepository
import org.example.kalkulationsprogramm.repository.FeiertagRepository
import org.example.kalkulationsprogramm.repository.LieferantenRepository
import org.example.kalkulationsprogramm.repository.MitarbeiterRepository
import org.example.kalkulationsprogramm.repository.ProduktkategorieRepository
import org.example.kalkulationsprogramm.repository.ProjektRepository
import org.example.kalkulationsprogramm.repository.ZeitbuchungRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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
    ): Map<String, Any> = mapOf("started" to false)

    fun stopZeiterfassung(token: String, originalEndeZeit: LocalDateTime?, idempotencyKey: String?): Map<String, Any> =
        mapOf("stopped" to false)

    fun startPause(token: String, originalZeit: LocalDateTime?, idempotencyKey: String?): Map<String, Any> =
        mapOf("paused" to false)

    fun getAktiveBuchung(token: String): Optional<Map<String, Any>> = Optional.empty()

    fun getHeuteGearbeitet(token: String): Map<String, Any> = mapOf("minuten" to 0)

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

    fun getSaldo(token: String, jahr: Int?, monat: Int?, gesamtBisHeute: Boolean?): Map<String, Any> =
        mapOf("saldoMinuten" to 0)

    fun getUrlaubsverfallWarnung(token: String): Map<String, Any> = emptyMap()

    fun getBuchungszeitfenster(token: String): Map<String, Any> = emptyMap()

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
}
