package org.example.kalkulationsprogramm.dto.Kunde

enum class KundeDuplikatGrund(
    val anzeigetext: String,
    val score: Int,
    private val hart: Boolean
) {
    EMAIL_GLEICH("Gleiche E-Mail-Adresse", 100, true),
    TELEFON_GLEICH("Gleiche Telefonnummer", 90, true),
    MOBILTELEFON_GLEICH("Gleiche Mobilnummer", 90, true),
    NAME_PLZ_GLEICH("Gleicher Name und PLZ", 60, false),
    NAME_STRASSE_GLEICH("Gleicher Name und Straße", 50, false);

    fun isHart(): Boolean = hart
}
