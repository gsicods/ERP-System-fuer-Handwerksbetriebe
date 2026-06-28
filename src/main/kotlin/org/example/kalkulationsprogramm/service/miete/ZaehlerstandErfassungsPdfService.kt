package org.example.kalkulationsprogramm.service.miete

import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.example.kalkulationsprogramm.domain.miete.Mietobjekt
import org.example.kalkulationsprogramm.domain.miete.Raum
import org.example.kalkulationsprogramm.domain.miete.Verbrauchsgegenstand
import org.example.kalkulationsprogramm.domain.miete.Zaehlerstand
import org.example.kalkulationsprogramm.exception.NotFoundException
import org.example.kalkulationsprogramm.repository.miete.MietobjektRepository
import org.example.kalkulationsprogramm.repository.miete.RaumRepository
import org.example.kalkulationsprogramm.repository.miete.VerbrauchsgegenstandRepository
import org.example.kalkulationsprogramm.repository.miete.ZaehlerstandRepository
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class ZaehlerstandErfassungsPdfService(
    private val mietobjektRepository: MietobjektRepository,
    private val raumRepository: RaumRepository,
    private val verbrauchsgegenstandRepository: VerbrauchsgegenstandRepository,
    private val zaehlerstandRepository: ZaehlerstandRepository,
) {
    fun generatePdf(mietobjektId: Long, zielJahr: Int?): ByteArray {
        val mietobjekt = mietobjektRepository.findById(mietobjektId)
            .orElseThrow { NotFoundException("Mietobjekt $mietobjektId nicht gefunden") }
        val jahr = zielJahr ?: LocalDate.now().year

        try {
            ByteArrayOutputStream().use { baos ->
                val document = Document(PageSize.A4, 36f, 36f, 40f, 40f)
                PdfWriter.getInstance(document, baos)
                document.open()

                addDocumentHeader(document, mietobjekt, jahr)

                val decimalFormat = NumberFormat.getNumberInstance(Locale.GERMANY)
                if (decimalFormat is DecimalFormat) {
                    decimalFormat.maximumFractionDigits = 4
                    decimalFormat.minimumFractionDigits = 0
                }
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

                val raeume = raumRepository.findByMietobjektOrderByNameAsc(mietobjekt)
                if (raeume.isEmpty()) {
                    document.add(Paragraph("Fuer dieses Mietobjekt sind keine Raeume angelegt.", TEXT_FONT))
                } else {
                    for (raum in raeume) {
                        addRoomSection(document, raum, jahr, decimalFormat, dateFormatter)
                    }
                }

                document.close()
                return baos.toByteArray()
            }
        } catch (e: DocumentException) {
            throw IllegalStateException("PDF-Erstellung fuer Zaehlerstands-Erfassung fehlgeschlagen", e)
        } catch (e: IOException) {
            throw IllegalStateException("PDF-Erstellung fuer Zaehlerstands-Erfassung fehlgeschlagen", e)
        }
    }

    @Throws(DocumentException::class)
    private fun addDocumentHeader(document: Document, mietobjekt: Mietobjekt, jahr: Int) {
        val title = Paragraph("Ablesebogen Zaehlerstaende", TITLE_FONT)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)

        document.add(Paragraph("Objekt: ${mietobjekt.name}", TEXT_FONT))

        val address = StringBuilder()
        if (hasText(mietobjekt.strasse)) {
            address.append(mietobjekt.strasse!!.trim())
        }
        if (hasText(mietobjekt.plz) || hasText(mietobjekt.ort)) {
            if (address.isNotEmpty()) {
                address.append(", ")
            }
            if (hasText(mietobjekt.plz)) {
                address.append(mietobjekt.plz!!.trim())
                address.append(' ')
            }
            if (hasText(mietobjekt.ort)) {
                address.append(mietobjekt.ort!!.trim())
            }
        }
        if (address.isNotEmpty()) {
            document.add(Paragraph("Adresse: $address", TEXT_FONT))
        }

        document.add(Paragraph("Ablesejahr: $jahr", TEXT_FONT))
        document.add(Paragraph("Erstellt am: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", TEXT_FONT))
        document.add(Paragraph(" "))
    }

    @Throws(DocumentException::class)
    private fun addRoomSection(
        document: Document,
        raum: Raum,
        zielJahr: Int,
        decimalFormat: NumberFormat,
        dateFormatter: DateTimeFormatter,
    ) {
        val header = Paragraph("Raum: ${raum.name}", SECTION_FONT)
        document.add(header)
        if (hasText(raum.beschreibung)) {
            val description = Paragraph("Beschreibung: ${raum.beschreibung!!.trim()}", TEXT_FONT)
            description.spacingAfter = 4f
            document.add(description)
        }

        val gegenstaende = verbrauchsgegenstandRepository.findByRaumOrderByNameAsc(raum)
        if (gegenstaende.isEmpty()) {
            document.add(Paragraph("Keine Verbrauchszaehler in diesem Raum.", TEXT_FONT))
            document.add(Paragraph(" "))
            return
        }

        val table = PdfPTable(floatArrayOf(2.2f, 1.3f, 1.3f, 1.1f, 1.3f, 1.3f, 1.8f))
        table.widthPercentage = 100f

        addHeaderCell(table, "Zaehler")
        addHeaderCell(table, "Einheit")
        addHeaderCell(table, "${zielJahr - 1} Stand")
        addHeaderCell(table, "Datum")
        addHeaderCell(table, "Verbrauch")
        addHeaderCell(table, "Neuer Stand")
        addHeaderCell(table, "Notizen / Besonderheiten")

        gegenstaende
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
            .forEach { gegenstand -> addMeterRow(table, gegenstand, zielJahr, decimalFormat, dateFormatter) }

        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addMeterRow(
        table: PdfPTable,
        gegenstand: Verbrauchsgegenstand,
        zielJahr: Int,
        decimalFormat: NumberFormat,
        dateFormatter: DateTimeFormatter,
    ) {
        val referenz = findReferenceReading(gegenstand, zielJahr)

        table.addCell(createValueCell(buildMeterLabel(gegenstand)))
        table.addCell(createValueCell(formatText(gegenstand.einheit)))
        table.addCell(createValueCell(formatDecimal(referenz?.stand, decimalFormat)))
        table.addCell(createValueCell(referenz?.stichtag?.format(dateFormatter) ?: "-"))
        table.addCell(createValueCell(formatDecimal(referenz?.verbrauch, decimalFormat)))
        table.addCell(createEmptyInputCell())
        table.addCell(createEmptyInputCell())
    }

    private fun findReferenceReading(gegenstand: Verbrauchsgegenstand, zielJahr: Int): Zaehlerstand? {
        val exaktVorjahr = zaehlerstandRepository
            .findByVerbrauchsgegenstandAndAbrechnungsJahr(gegenstand, zielJahr - 1)
        if (exaktVorjahr.isPresent) {
            return exaktVorjahr.get()
        }
        return zaehlerstandRepository.findByVerbrauchsgegenstandOrderByAbrechnungsJahrDesc(gegenstand)
            .stream()
            .findFirst()
            .orElse(null)
    }

    private fun createValueCell(text: String?): PdfPCell {
        val cell = PdfPCell(Phrase(text ?: "-", TEXT_FONT))
        cell.setPadding(6f)
        return cell
    }

    private fun createEmptyInputCell(): PdfPCell {
        val cell = PdfPCell(Phrase(" ", TEXT_FONT))
        cell.setPadding(12f)
        return cell
    }

    private fun addHeaderCell(table: PdfPTable, text: String) {
        val cell = PdfPCell(Phrase(text, HEADER_FONT))
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.setPadding(6f)
        table.addCell(cell)
    }

    private fun buildMeterLabel(gegenstand: Verbrauchsgegenstand): String {
        val label = StringBuilder()
        if (hasText(gegenstand.name)) {
            label.append(gegenstand.name!!.trim())
        }
        if (gegenstand.verbrauchsart != null) {
            if (label.isNotEmpty()) {
                label.append(" - ")
            }
            label.append(gegenstand.verbrauchsart!!.name)
        }
        if (hasText(gegenstand.seriennummer)) {
            if (label.isNotEmpty()) {
                label.append(" - ")
            }
            label.append("SN ").append(gegenstand.seriennummer!!.trim())
        }
        return if (label.isNotEmpty()) label.toString() else "-"
    }

    private fun formatDecimal(value: BigDecimal?, decimalFormat: NumberFormat): String =
        value?.let(decimalFormat::format) ?: "-"

    private fun formatText(value: String?): String =
        if (hasText(value)) value!!.trim() else "-"

    private fun hasText(value: String?): Boolean =
        !value.isNullOrBlank()

    companion object {
        private val TITLE_FONT = Font(Font.HELVETICA, 15f, Font.BOLD)
        private val SECTION_FONT = Font(Font.HELVETICA, 12f, Font.BOLD)
        private val TEXT_FONT = Font(Font.HELVETICA, 10f, Font.NORMAL)
        private val HEADER_FONT = Font(Font.HELVETICA, 10f, Font.BOLD)
    }
}
