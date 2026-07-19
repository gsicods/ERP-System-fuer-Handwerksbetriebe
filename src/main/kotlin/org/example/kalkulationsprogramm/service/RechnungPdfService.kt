package org.example.kalkulationsprogramm.service

import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Document
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.stereotype.Service
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
open class RechnungPdfService {
    data class LayoutDto(
        val page1Rect: RectDto,
        val page2Rect: RectDto,
        val headerRect: RectDto,
        val footerRect: RectDto,
        val logoPath: String?,
    ) {
        fun page1Rect(): RectDto = page1Rect
        fun page2Rect(): RectDto = page2Rect
        fun headerRect(): RectDto = headerRect
        fun footerRect(): RectDto = footerRect
        fun logoPath(): String? = logoPath
    }

    data class RectDto(
        val llx: Float,
        val lly: Float,
        val urx: Float,
        val ury: Float,
    ) {
        fun llx(): Float = llx
        fun lly(): Float = lly
        fun urx(): Float = urx
        fun ury(): Float = ury
        fun toRectangle(): Rectangle = Rectangle(llx, lly, urx, ury)
    }

    data class RechnungDto(
        val layout: LayoutDto,
        val kopfdaten: KopfdatenDto,
        val contentBlocks: List<ContentBlockDto>?,
        val formBlocks: List<FormBlockDto>?,
        val schlusstext: String?,
        val backgroundImagePage1: String?,
        val backgroundImagePage2: String?,
        val globalRabattProzent: BigDecimal? = null,
        val abrechnungsverlauf: AbrechnungsverlaufPdfDto? = null,
        val betragNetto: BigDecimal? = null,
        val abschlagInfo: AbschlagInfoPdfDto? = null,
    ) {
        fun layout(): LayoutDto = layout
        fun kopfdaten(): KopfdatenDto = kopfdaten
        fun contentBlocks(): List<ContentBlockDto>? = contentBlocks
        fun formBlocks(): List<FormBlockDto>? = formBlocks
        fun schlusstext(): String? = schlusstext
        fun backgroundImagePage1(): String? = backgroundImagePage1
        fun backgroundImagePage2(): String? = backgroundImagePage2
        fun globalRabattProzent(): BigDecimal? = globalRabattProzent
        fun abrechnungsverlauf(): AbrechnungsverlaufPdfDto? = abrechnungsverlauf
        fun betragNetto(): BigDecimal? = betragNetto
        fun abschlagInfo(): AbschlagInfoPdfDto? = abschlagInfo
    }

    data class AbrechnungsverlaufPdfDto(
        val basisdokumentNummer: String?,
        val basisdokumentTyp: String?,
        val basisdokumentDatum: LocalDate?,
        val basisdokumentBetragNetto: BigDecimal?,
        val positionen: List<AbrechnungspositionPdfDto>?,
    ) {
        fun basisdokumentNummer(): String? = basisdokumentNummer
        fun basisdokumentTyp(): String? = basisdokumentTyp
        fun basisdokumentDatum(): LocalDate? = basisdokumentDatum
        fun basisdokumentBetragNetto(): BigDecimal? = basisdokumentBetragNetto
        fun positionen(): List<AbrechnungspositionPdfDto>? = positionen
    }

    data class AbrechnungspositionPdfDto(
        val dokumentNummer: String?,
        val typ: String?,
        val datum: LocalDate?,
        val betragNetto: BigDecimal?,
        val abschlagsNummer: Int?,
    ) {
        fun dokumentNummer(): String? = dokumentNummer
        fun typ(): String? = typ
        fun datum(): LocalDate? = datum
        fun betragNetto(): BigDecimal? = betragNetto
        fun abschlagsNummer(): Int? = abschlagsNummer
    }

    data class AbschlagInfoPdfDto(
        val modus: String?,
        val eingabeWert: BigDecimal?,
    ) {
        fun modus(): String? = modus
        fun eingabeWert(): BigDecimal? = eingabeWert
    }

    data class ContentBlockDto(
        val type: String?,
        val text: String?,
        val fett: Boolean,
        val fontSize: Int,
        val pos: String?,
        val beschreibung: String?,
        val beschreibungHtml: String?,
        val menge: BigDecimal?,
        val einheit: String?,
        val einzelpreis: BigDecimal?,
        val gesamt: BigDecimal?,
        val optional: Boolean,
        val sectionLabel: String?,
        val rabattProzent: BigDecimal?,
    ) {
        fun type(): String? = type
        fun text(): String? = text
        fun fett(): Boolean = fett
        fun fontSize(): Int = fontSize
        fun pos(): String? = pos
        fun beschreibung(): String? = beschreibung
        fun beschreibungHtml(): String? = beschreibungHtml
        fun menge(): BigDecimal? = menge
        fun einheit(): String? = einheit
        fun einzelpreis(): BigDecimal? = einzelpreis
        fun gesamt(): BigDecimal? = gesamt
        fun optional(): Boolean = optional
        fun sectionLabel(): String? = sectionLabel
        fun rabattProzent(): BigDecimal? = rabattProzent
        fun isText(): Boolean = type == "TEXT"
        fun isService(): Boolean = type == "SERVICE"
        fun isSeparator(): Boolean = type == "SEPARATOR"
        fun isSectionHeader(): Boolean = type == "SECTION_HEADER"
        fun isSubtotal(): Boolean = type == "SUBTOTAL"
    }

    data class KopfdatenDto(
        val rechnungsnummer: String?,
        val rechnungsDatum: LocalDate?,
        val leistungsDatum: LocalDate?,
        val kundenName: String?,
        val kundenAdresse: String?,
        val betreff: String?,
        val kundennummer: String?,
        val dokumentTyp: String?,
        val bezugsdokument: String?,
        val projektnummer: String?,
        val bauvorhaben: String?,
        val bezugsdokumentTyp: String? = null,
        val bezugsdokumentDatum: String? = null,
        val zahlungszielTage: Int? = null,
    ) {
        fun rechnungsnummer(): String? = rechnungsnummer
        fun rechnungsDatum(): LocalDate? = rechnungsDatum
        fun leistungsDatum(): LocalDate? = leistungsDatum
        fun kundenName(): String? = kundenName
        fun kundenAdresse(): String? = kundenAdresse
        fun betreff(): String? = betreff
        fun kundennummer(): String? = kundennummer
        fun dokumentTyp(): String? = dokumentTyp
        fun bezugsdokument(): String? = bezugsdokument
        fun projektnummer(): String? = projektnummer
        fun bauvorhaben(): String? = bauvorhaben
        fun bezugsdokumentTyp(): String? = bezugsdokumentTyp
        fun bezugsdokumentDatum(): String? = bezugsdokumentDatum
        fun zahlungszielTage(): Int? = zahlungszielTage
    }

    data class FormBlockDto(
        val id: String?,
        val type: String?,
        val page: Int?,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val content: String?,
        val styles: Map<String, Any>?,
    ) {
        fun id(): String? = id
        fun type(): String? = type
        fun page(): Int? = page
        fun x(): Float = x
        fun y(): Float = y
        fun width(): Float = width
        fun height(): Float = height
        fun content(): String? = content
        fun styles(): Map<String, Any>? = styles
    }

    open fun generatePdf(data: RechnungDto, out: OutputStream) {
        val document = Document(PageSize.A4, 42f, 42f, 48f, 44f)
        try {
            PdfWriter.getInstance(document, out)
            document.open()
            addHeader(document, data.kopfdaten)
            addContentBlocks(document, data.contentBlocks.orEmpty())
            addTotals(document, data)
            data.schlusstext?.takeIf { it.isNotBlank() }?.let {
                document.add(Paragraph(stripHtml(it), FONT_NORMAL).apply { spacingBefore = 12f })
            }
            addFooter(document, data.kopfdaten)
        } finally {
            if (document.isOpen) {
                document.close()
            }
        }
    }

    open fun generatePdfBytes(data: RechnungDto): ByteArray =
        ByteArrayOutputStream().use {
            generatePdf(data, it)
            it.toByteArray()
        }

    private fun addHeader(document: Document, kopf: KopfdatenDto) {
        val title = kopf.dokumentTyp?.takeIf { it.isNotBlank() } ?: "Dokument"
        document.add(Paragraph(title, FONT_TITLE).apply {
            alignment = Element.ALIGN_RIGHT
            spacingAfter = 10f
        })

        val meta = PdfPTable(floatArrayOf(1.2f, 1f)).apply {
            widthPercentage = 100f
            setSpacingAfter(16f)
        }
        meta.addCell(noBorderCell(kopf.kundenName.orEmpty() + "\n" + kopf.kundenAdresse.orEmpty(), FONT_NORMAL, Element.ALIGN_LEFT))
        meta.addCell(
            noBorderCell(
                listOfNotNull(
                    kopf.rechnungsnummer?.let { "Nr.: $it" },
                    kopf.rechnungsDatum?.format(DATE_DE)?.let { "Datum: $it" },
                    kopf.kundennummer?.let { "Kundennr.: $it" },
                    kopf.projektnummer?.takeIf { it.isNotBlank() }?.let { "Projekt: $it" },
                    kopf.bezugsdokument?.takeIf { it.isNotBlank() }?.let { "Bezug: $it" },
                ).joinToString("\n"),
                FONT_NORMAL,
                Element.ALIGN_RIGHT,
            ),
        )
        document.add(meta)

        kopf.betreff?.takeIf { it.isNotBlank() }?.let {
            document.add(Paragraph(stripHtml(it), FONT_BOLD).apply { spacingAfter = 8f })
        }
        kopf.bauvorhaben?.takeIf { it.isNotBlank() }?.let {
            document.add(Paragraph("Bauvorhaben: ${stripHtml(it)}", FONT_NORMAL).apply { spacingAfter = 8f })
        }
    }

    private fun addContentBlocks(document: Document, blocks: List<ContentBlockDto>) {
        var table: PdfPTable? = null
        blocks.forEach { block ->
            when {
                block.isText() -> {
                    table = flushTable(document, table)
                    block.text?.takeIf { it.isNotBlank() }?.let {
                        document.add(Paragraph(stripHtml(it), if (block.fett) FONT_BOLD else FONT_NORMAL).apply {
                            spacingBefore = 6f
                            spacingAfter = 6f
                        })
                    }
                }
                block.isSectionHeader() -> {
                    table = flushTable(document, table)
                    document.add(Paragraph(block.sectionLabel ?: "", FONT_HEADER).apply {
                        spacingBefore = 10f
                        spacingAfter = 4f
                    })
                }
                block.isSeparator() -> {
                    table = flushTable(document, table)
                    document.add(Paragraph(" ").apply { spacingAfter = 4f })
                }
                block.isService() -> {
                    if (table == null) table = createPositionTable()
                    addServiceRow(requireNotNull(table), block)
                }
            }
        }
        flushTable(document, table)
    }

    private fun addTotals(document: Document, data: RechnungDto) {
        val netto = data.betragNetto ?: sumNetto(data.contentBlocks.orEmpty())
        val rabatt = data.globalRabattProzent?.takeIf { it.signum() > 0 } ?: BigDecimal.ZERO
        val nettoNachRabatt = if (rabatt.signum() > 0) {
            netto.multiply(BigDecimal.ONE.subtract(rabatt.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)))
        } else {
            netto
        }.setScale(2, RoundingMode.HALF_UP)
        val mwstSatz = BigDecimal("0.19")
        val mwst = nettoNachRabatt.multiply(mwstSatz).setScale(2, RoundingMode.HALF_UP)
        val brutto = nettoNachRabatt.add(mwst).setScale(2, RoundingMode.HALF_UP)

        val table = PdfPTable(floatArrayOf(1f, 0.35f)).apply {
            widthPercentage = 48f
            horizontalAlignment = Element.ALIGN_RIGHT
            setSpacingBefore(12f)
        }
        addTotalRow(table, "Netto", netto)
        if (rabatt.signum() > 0) {
            addTotalRow(table, "Rabatt ${rabatt.stripTrailingZeros().toPlainString()}%", netto.subtract(nettoNachRabatt).negate())
            addTotalRow(table, "Netto nach Rabatt", nettoNachRabatt)
        }
        data.abrechnungsverlauf?.positionen.orEmpty()
            .filter { it.betragNetto != null }
            .forEach { addTotalRow(table, "abzgl. ${it.typ ?: "Abschlag"} ${it.dokumentNummer ?: ""}".trim(), it.betragNetto!!.negate()) }
        addTotalRow(table, "MwSt. 19%", mwst)
        addTotalRow(table, "Brutto", brutto, FONT_BOLD)
        document.add(table)
    }

    private fun addFooter(document: Document, kopf: KopfdatenDto) {
        val zahlungsziel = kopf.zahlungszielTage
        if (zahlungsziel != null && zahlungsziel > 0) {
            document.add(Paragraph("Zahlungsziel: $zahlungsziel Tage", FONT_SMALL).apply {
                spacingBefore = 14f
            })
        }
    }

    private fun createPositionTable(): PdfPTable =
        PdfPTable(floatArrayOf(0.14f, 0.42f, 0.12f, 0.12f, 0.16f, 0.16f)).apply {
            widthPercentage = 100f
            setSpacingBefore(6f)
            setSpacingAfter(8f)
            listOf("Pos.", "Beschreibung", "Menge", "Einheit", "Einzel", "Gesamt").forEach {
                addCell(headerCell(it))
            }
        }

    private fun addServiceRow(table: PdfPTable, block: ContentBlockDto) {
        val beschreibung = listOfNotNull(block.beschreibung, block.beschreibungHtml?.let(::stripHtml))
            .joinToString("\n")
            .ifBlank { "-" }
        table.addCell(simpleCell(block.pos.orEmpty()))
        table.addCell(simpleCell(beschreibung))
        table.addCell(simpleCell(formatNumber(block.menge), Element.ALIGN_RIGHT))
        table.addCell(simpleCell(block.einheit.orEmpty()))
        table.addCell(simpleCell(formatCurrency(block.einzelpreis), Element.ALIGN_RIGHT))
        table.addCell(simpleCell(formatCurrency(block.gesamt ?: berechneGesamt(block)), Element.ALIGN_RIGHT))
    }

    private fun flushTable(document: Document, table: PdfPTable?): PdfPTable? {
        if (table != null && table.rows.size > 1) {
            document.add(table)
        }
        return null
    }

    private fun addTotalRow(table: PdfPTable, label: String, value: BigDecimal, font: Font = FONT_NORMAL) {
        table.addCell(noBorderCell(label, font, Element.ALIGN_RIGHT))
        table.addCell(noBorderCell(formatCurrency(value), font, Element.ALIGN_RIGHT))
    }

    private fun sumNetto(blocks: List<ContentBlockDto>): BigDecimal =
        blocks.filter { it.isService() && !it.optional }
            .fold(BigDecimal.ZERO) { acc, block -> acc.add(block.gesamt ?: berechneGesamt(block)) }
            .setScale(2, RoundingMode.HALF_UP)

    private fun berechneGesamt(block: ContentBlockDto): BigDecimal {
        val menge = block.menge ?: BigDecimal.ONE
        val preis = block.einzelpreis ?: BigDecimal.ZERO
        val rabatt = block.rabattProzent ?: BigDecimal.ZERO
        val basis = menge.multiply(preis)
        return if (rabatt.signum() > 0) {
            basis.multiply(BigDecimal.ONE.subtract(rabatt.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)))
        } else {
            basis
        }.setScale(2, RoundingMode.HALF_UP)
    }

    private fun headerCell(text: String): PdfPCell =
        PdfPCell(Phrase(text, FONT_BOLD)).apply {
            backgroundColor = Color(235, 235, 235)
            horizontalAlignment = Element.ALIGN_CENTER
            setPadding(5f)
        }

    private fun simpleCell(text: String?, alignment: Int = Element.ALIGN_LEFT): PdfPCell =
        PdfPCell(Phrase(text.orEmpty(), FONT_NORMAL)).apply {
            horizontalAlignment = alignment
            verticalAlignment = Element.ALIGN_TOP
            setPadding(5f)
        }

    private fun noBorderCell(text: String?, font: Font, alignment: Int): PdfPCell =
        PdfPCell(Phrase(text.orEmpty(), font)).apply {
            border = Rectangle.NO_BORDER
            horizontalAlignment = alignment
            setPadding(3f)
        }

    private fun stripHtml(value: String): String =
        value.replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("[ \\t]+"), " ")
            .trim()

    private fun formatCurrency(value: BigDecimal?): String =
        CURRENCY.format(value ?: BigDecimal.ZERO)

    private fun formatNumber(value: BigDecimal?): String =
        if (value == null) "" else NUMBER.format(value)

    companion object {
        private val FONT_NORMAL: Font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 10f)
        private val FONT_BOLD: Font = FontFactory.getFont(FontFactory.TIMES_BOLD, 10f)
        private val FONT_SMALL: Font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 8f)
        private val FONT_HEADER: Font = FontFactory.getFont(FontFactory.TIMES_BOLD, 11f)
        private val FONT_TITLE: Font = FontFactory.getFont(FontFactory.TIMES_BOLD, 16f)
        private val DATE_DE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val CURRENCY: NumberFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)
        private val NUMBER: NumberFormat = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 3
        }

        @JvmStatic
        fun getDefaultLayout(): LayoutDto =
            LayoutDto(
                RectDto(50f, 120f, 550f, 600f),
                RectDto(50f, 50f, 550f, 780f),
                RectDto(50f, 750f, 550f, 840f),
                RectDto(50f, 20f, 550f, 100f),
                null,
            )

        @JvmStatic
        fun convertFormBlockToRect(block: FormBlockDto, pageWidthPt: Float, pageHeightPt: Float): RectDto {
            val llx = block.x
            val urx = block.x + block.width
            val ury = pageHeightPt - block.y
            val lly = ury - block.height
            return RectDto(llx, lly, urx, ury)
        }

        @JvmStatic
        fun createLayoutFromFormBlocks(
            blocks: List<FormBlockDto>?,
            pageWidthPt: Float,
            pageHeightPt: Float,
        ): LayoutDto {
            val page1 = blocks.orEmpty().firstOrNull { it.type == "table" && (it.page == null || it.page == 1) }
                ?.let { convertFormBlockToRect(it, pageWidthPt, pageHeightPt) }
                ?: RectDto(50f, 120f, 550f, 600f)
            val page2 = blocks.orEmpty().firstOrNull { it.type == "table" && it.page == 2 }
                ?.let { convertFormBlockToRect(it, pageWidthPt, pageHeightPt) }
                ?: RectDto(50f, 50f, 550f, 780f)
            return LayoutDto(page1, page2, RectDto(50f, 750f, 550f, 840f), RectDto(50f, 20f, 550f, 100f), null)
        }
    }
}
