package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Projekt
import org.example.kalkulationsprogramm.domain.ProjektGeschaeftsdokument
import org.example.kalkulationsprogramm.dto.Zugferd.ZugferdDaten
import org.springframework.stereotype.Service
import java.lang.reflect.Field

@Service
class ZugferdConverterService {
    fun convertRechnung(projekt: Projekt?, rechnung: ProjektGeschaeftsdokument?): ZugferdDaten {
        val daten = ZugferdDaten()
        if (projekt != null) {
            daten.kundenName = projekt.readField("kunde") as? String
            daten.kundennummer = projekt.readField("kundennummer") as? String
        }
        if (rechnung != null) {
            daten.rechnungsnummer = rechnung.readField("dokumentid") as? String
            daten.geschaeftsdokumentart = rechnung.readField("geschaeftsdokumentart") as? String
            daten.rechnungsdatum = rechnung.readField("rechnungsdatum") as? java.time.LocalDate
            daten.faelligkeitsdatum = rechnung.readField("faelligkeitsdatum") as? java.time.LocalDate
            daten.betrag = rechnung.readField("bruttoBetrag") as? java.math.BigDecimal
        }
        return daten
    }

    private fun Any.readField(name: String): Any? {
        val field: Field = findField(javaClass, name)
        field.isAccessible = true
        return field.get(this)
    }

    private fun findField(type: Class<*>, name: String): Field =
        try {
            type.getDeclaredField(name)
        } catch (_: NoSuchFieldException) {
            findField(type.superclass, name)
        }
}
