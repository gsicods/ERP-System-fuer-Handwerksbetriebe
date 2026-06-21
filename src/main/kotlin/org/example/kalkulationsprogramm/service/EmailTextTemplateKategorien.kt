package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.EmailTextTemplateKategorie

object EmailTextTemplateKategorien {
    private val zuordnung = mapOf(
        "ANGEBOT" to EmailTextTemplateKategorie.DOKUMENT,
        "NACHTRAGSANGEBOT" to EmailTextTemplateKategorie.DOKUMENT,
        "AUFTRAGSBESTAETIGUNG" to EmailTextTemplateKategorie.DOKUMENT,
        "ZEICHNUNG" to EmailTextTemplateKategorie.DOKUMENT,
        "RECHNUNG" to EmailTextTemplateKategorie.DOKUMENT,
        "TEILRECHNUNG" to EmailTextTemplateKategorie.DOKUMENT,
        "ABSCHLAGSRECHNUNG" to EmailTextTemplateKategorie.DOKUMENT,
        "SCHLUSSRECHNUNG" to EmailTextTemplateKategorie.DOKUMENT,
        "GUTSCHRIFT" to EmailTextTemplateKategorie.DOKUMENT,
        "STORNORECHNUNG" to EmailTextTemplateKategorie.DOKUMENT,
        "ZAHLUNGSERINNERUNG" to EmailTextTemplateKategorie.MAHNWESEN,
        "ERSTE_MAHNUNG" to EmailTextTemplateKategorie.MAHNWESEN,
        "ZWEITE_MAHNUNG" to EmailTextTemplateKategorie.MAHNWESEN,
        "MAHNUNG" to EmailTextTemplateKategorie.MAHNWESEN,
        "WEBSITE_ANFRAGE_BESTAETIGUNG" to EmailTextTemplateKategorie.WEBSITE
    )

    @JvmStatic
    fun kategorieFuer(dokumentTyp: String?): EmailTextTemplateKategorie =
        dokumentTyp?.trim()?.uppercase()?.let { zuordnung[it] } ?: EmailTextTemplateKategorie.SYSTEM
}
