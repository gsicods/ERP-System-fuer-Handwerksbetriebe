package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import org.example.kalkulationsprogramm.domain.DokumentFreigabe
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository
import org.example.kalkulationsprogramm.repository.DokumentFreigabeRepository
import org.example.kalkulationsprogramm.service.RechnungPdfService.FormBlockDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@Service
open class AutoAuftragsbestaetigungVersandService(
    private val ausgangsGeschaeftsDokumentRepository: AusgangsGeschaeftsDokumentRepository? = null,
    private val dokumentFreigabeRepository: DokumentFreigabeRepository? = null,
    private val projektEmailArchivService: ProjektEmailArchivService? = null,
) {
    @Transactional
    open fun versendeNachAnnahme(abId: Long?, empfaenger: String?, freigabeUuid: String?): Boolean {
        val id = abId ?: return false
        val ab = ausgangsGeschaeftsDokumentRepository?.findById(id)?.orElse(null) ?: return false
        val freigabe = freigabeUuid?.let { dokumentFreigabeRepository?.findByUuid(it)?.orElse(null) }
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
        markiereAlsVersendet(ab)
        log.info("Auto-AB {} als versendet markiert", ab.dokumentNummer)
        return true
    }

    @Transactional
    protected open fun markiereAlsVersendet(ab: AusgangsGeschaeftsDokument) {
        val id = ab.id ?: return
        val frisch = ausgangsGeschaeftsDokumentRepository?.findById(id)?.orElse(null) ?: return
        frisch.versandDatum = LocalDate.now()
        ausgangsGeschaeftsDokumentRepository.save(frisch)
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

        @JvmStatic
        fun aufloesePlatzhalter(text: String?, ctx: Map<String, String>): String =
            ctx.entries.fold(text.orEmpty()) { acc, (key, value) ->
                acc.replace("{{${key.uppercase()}}}", value)
                    .replace("{{${key.lowercase()}}}", value)
            }

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
