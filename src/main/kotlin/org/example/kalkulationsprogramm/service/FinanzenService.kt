package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp
import org.example.kalkulationsprogramm.domain.Beleg
import org.example.kalkulationsprogramm.domain.BelegStatus
import org.example.kalkulationsprogramm.domain.Zahlung
import org.example.kalkulationsprogramm.domain.ZahlungRichtung
import org.example.kalkulationsprogramm.domain.ZahlungStatus
import org.example.kalkulationsprogramm.dto.finanzen.FinanzenDashboardDto
import org.example.kalkulationsprogramm.dto.finanzen.OffenerPostenDto
import org.example.kalkulationsprogramm.dto.finanzen.ZahlungDto
import org.example.kalkulationsprogramm.dto.finanzen.ZahlungErfassenDto
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository
import org.example.kalkulationsprogramm.repository.BelegRepository
import org.example.kalkulationsprogramm.repository.ZahlungRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class FinanzenService(
    private val dokumentRepository: AusgangsGeschaeftsDokumentRepository,
    private val belegRepository: BelegRepository,
    private val zahlungRepository: ZahlungRepository
) {
    @Transactional(readOnly = true)
    fun dashboard(jahr: Int?, monat: Int?): FinanzenDashboardDto {
        val von = zeitraumVon(jahr, monat)
        val bis = zeitraumBis(von, monat)
        val dokumente = dokumentRepository.findByDatumBetweenOrderByDatumDesc(von, bis)
        val belege = belegRepository.findValidierteImZeitraumFuerExport(von, bis)
        val umsatz = dokumente
            .filter { istAktiveRechnung(it) }
            .map { nn(prop(it, "betragBrutto")) }
            .fold(BigDecimal.ZERO, BigDecimal::add)
        val kosten = belege
            .map { betragBelegBrutto(it) }
            .fold(BigDecimal.ZERO, BigDecimal::add)
        val zahlungseingaenge = nn(zahlungRepository.summeByRichtungImZeitraum(ZahlungRichtung.EINGANG, von, bis))
        val zahlungsausgaenge = nn(zahlungRepository.summeByRichtungImZeitraum(ZahlungRichtung.AUSGANG, von, bis))
        val offenePosten = offenePosten()
        val offeneAusgang = offenePosten
            .filter { it.typ == "AUSGANGSRECHNUNG" }
            .map { it.offen }
            .fold(BigDecimal.ZERO, BigDecimal::add)
        val offeneEingang = offenePosten
            .filter { it.typ == "EINGANGSBELEG" }
            .map { it.offen }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        return FinanzenDashboardDto(
            von = von,
            bis = bis,
            umsatzBrutto = umsatz,
            eingangsKostenBrutto = kosten,
            zahlungseingaenge = zahlungseingaenge,
            zahlungsausgaenge = zahlungsausgaenge,
            offenerAusgangBrutto = offeneAusgang,
            offeneEingangsBelegeBrutto = offeneEingang,
            liquiditaet = zahlungseingaenge.subtract(zahlungsausgaenge),
            ergebnisBrutto = umsatz.subtract(kosten),
            offeneAusgangsrechnungen = offenePosten.count { it.typ == "AUSGANGSRECHNUNG" }.toLong(),
            offeneEingangsbelege = offenePosten.count { it.typ == "EINGANGSBELEG" }.toLong(),
            offenePosten = offenePosten.take(30)
        )
    }

    @Transactional(readOnly = true)
    fun offenePosten(): List<OffenerPostenDto> {
        val result = mutableListOf<OffenerPostenDto>()
        val heute = LocalDate.now()

        for (d in dokumentRepository.findAllByOrderByDatumDesc()) {
            if (!istAktiveRechnung(d)) continue
            val brutto = nn(prop(d, "betragBrutto"))
            val bezahlt = nn(zahlungRepository.summeByAusgangsDokument(prop(d, "id")))
            val offen = brutto.subtract(bezahlt)
            if (offen.signum() <= 0) continue
            val datum: LocalDate = prop(d, "datum") ?: continue
            val zahlungszielTage: Int = prop(d, "zahlungszielTage") ?: 14
            val faellig = datum.plusDays(zahlungszielTage.toLong())
            result += OffenerPostenDto(
                typ = "AUSGANGSRECHNUNG",
                id = prop(d, "id"),
                nummer = prop(d, "dokumentNummer"),
                name = prop<Any>(d, "kunde")?.let { prop<String>(it, "name") },
                datum = datum,
                faelligAm = faellig,
                brutto = brutto,
                bezahlt = bezahlt,
                offen = offen,
                ueberfaellig = faellig.isBefore(heute)
            )
        }

        for (b in belegRepository.findValidierteImZeitraumFuerExport(null, null)) {
            val brutto = betragBelegBrutto(b)
            val bezahlt = nn(zahlungRepository.summeByBeleg(prop(b, "id")))
            val offen = brutto.subtract(bezahlt)
            if (offen.signum() <= 0) continue
            val datum = prop<LocalDate>(b, "belegDatum") ?: prop<LocalDateTime>(b, "uploadDatum")?.toLocalDate() ?: continue
            result += OffenerPostenDto(
                typ = "EINGANGSBELEG",
                id = prop(b, "id"),
                nummer = prop(b, "belegNummer"),
                name = prop<Any>(b, "lieferant")?.let { prop<String>(it, "lieferantenname") } ?: prop(b, "beschreibung"),
                datum = datum,
                faelligAm = datum,
                brutto = brutto,
                bezahlt = bezahlt,
                offen = offen,
                ueberfaellig = false
            )
        }

        return result.sortedWith(
            compareByDescending<OffenerPostenDto> { it.ueberfaellig }
                .thenBy(nullsLast()) { it.faelligAm }
        )
    }

    @Transactional
    fun erfasseAusgangszahlung(dokumentId: Long, dto: ZahlungErfassenDto?): ZahlungDto {
        val dokument = dokumentRepository.findById(dokumentId)
            .orElseThrow { IllegalArgumentException("Ausgangsdokument nicht gefunden") }
        if (!istAktiveRechnung(dokument)) {
            throw IllegalArgumentException("Zahlungen sind nur fuer aktive Rechnungen moeglich")
        }
        val zahlung = zahlungRepository.save(neueZahlung(dto, ZahlungRichtung.EINGANG).apply {
            ausgangsDokument = dokument
        })
        aktualisiereProjektBezahlt(dokument)
        return toDto(zahlung)
    }

    @Transactional
    fun erfasseBelegzahlung(belegId: Long, dto: ZahlungErfassenDto?): ZahlungDto {
        val beleg = belegRepository.findById(belegId)
            .orElseThrow { IllegalArgumentException("Beleg nicht gefunden") }
        if (prop<BelegStatus>(beleg, "status") != BelegStatus.VALIDIERT) {
            throw IllegalArgumentException("Zahlungen sind nur fuer validierte Belege moeglich")
        }
        return toDto(zahlungRepository.save(neueZahlung(dto, ZahlungRichtung.AUSGANG).apply {
            this.beleg = beleg
        }))
    }

    @Transactional(readOnly = true)
    fun zahlungenAusgang(dokumentId: Long): List<ZahlungDto> =
        zahlungRepository.findByAusgangsDokumentIdAndStatusOrderByZahlungsdatumDesc(dokumentId, ZahlungStatus.ERFASST)
            .map { toDto(it) }

    @Transactional(readOnly = true)
    fun zahlungenBeleg(belegId: Long): List<ZahlungDto> =
        zahlungRepository.findByBelegIdAndStatusOrderByZahlungsdatumDesc(belegId, ZahlungStatus.ERFASST)
            .map { toDto(it) }

    private fun neueZahlung(dto: ZahlungErfassenDto?, richtung: ZahlungRichtung): Zahlung {
        if (dto?.betrag == null || dto.betrag.signum() <= 0) {
            throw IllegalArgumentException("Betrag muss groesser als 0 sein")
        }
        return Zahlung().apply {
            this.richtung = richtung
            betrag = dto.betrag
            zahlungsdatum = dto.zahlungsdatum ?: LocalDate.now()
            zahlungsart = blankToNull(dto.zahlungsart)
            verwendungszweck = blankToNull(dto.verwendungszweck)
        }
    }

    private fun aktualisiereProjektBezahlt(dokument: AusgangsGeschaeftsDokument) {
        val projekt: Any = prop(dokument, "projekt") ?: return
        val offen = nn(prop(dokument, "betragBrutto")).subtract(nn(zahlungRepository.summeByAusgangsDokument(prop(dokument, "id"))))
        if (offen.signum() <= 0) {
            setProp(projekt, "bezahlt", true)
            dokumentRepository.save(dokument)
        }
    }

    private fun istAktiveRechnung(d: AusgangsGeschaeftsDokument?): Boolean =
        d != null &&
            prop<AusgangsGeschaeftsDokumentTyp>(d, "typ") in RECHNUNGSTYPEN &&
            prop<Boolean>(d, "storniert") != true &&
            prop<BigDecimal>(d, "betragBrutto") != null

    private fun betragBelegBrutto(b: Beleg?): BigDecimal =
        b?.let { prop<BigDecimal>(it, "betragFirmaBrutto") } ?: nn(b?.let { prop<BigDecimal>(it, "betragBrutto") })

    private fun zeitraumVon(jahr: Int?, monat: Int?): LocalDate =
        LocalDate.of(jahr ?: LocalDate.now().year, monat ?: 1, 1)

    private fun zeitraumBis(von: LocalDate, monat: Int?): LocalDate =
        if (monat != null) von.withDayOfMonth(von.lengthOfMonth()) else von.withMonth(12).withDayOfMonth(31)

    private fun toDto(z: Zahlung): ZahlungDto =
        ZahlungDto(
            id = z.id,
            richtung = z.richtung,
            status = z.status,
            zahlungsdatum = z.zahlungsdatum,
            betrag = z.betrag,
            zahlungsart = z.zahlungsart,
            verwendungszweck = z.verwendungszweck,
            ausgangsDokumentId = z.ausgangsDokument?.let { prop<Long>(it, "id") },
            belegId = z.beleg?.let { prop<Long>(it, "id") },
            erfasstAm = z.erfasstAm
        )

    companion object {
        private val RECHNUNGSTYPEN = setOf(
            AusgangsGeschaeftsDokumentTyp.RECHNUNG,
            AusgangsGeschaeftsDokumentTyp.TEILRECHNUNG,
            AusgangsGeschaeftsDokumentTyp.ABSCHLAGSRECHNUNG,
            AusgangsGeschaeftsDokumentTyp.SCHLUSSRECHNUNG
        )

        private fun nn(value: BigDecimal?): BigDecimal = value ?: BigDecimal.ZERO

        private fun blankToNull(value: String?): String? =
            value?.trim()?.takeIf { it.isNotEmpty() }

        @Suppress("UNCHECKED_CAST")
        private fun <T> prop(target: Any, property: String): T? {
            val suffix = property.replaceFirstChar { it.uppercaseChar() }
            val getterNames = listOf("get$suffix", "is$suffix")
            val getter = getterNames.firstNotNullOfOrNull { name ->
                target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            } ?: return null
            return getter.invoke(target) as T?
        }

        private fun setProp(target: Any, property: String, value: Any?) {
            val suffix = property.replaceFirstChar { it.uppercaseChar() }
            val setter = target.javaClass.methods.firstOrNull { it.name == "set$suffix" && it.parameterCount == 1 }
                ?: return
            setter.invoke(target, value)
        }
    }
}
