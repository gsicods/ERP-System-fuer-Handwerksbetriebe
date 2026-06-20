package org.example.kalkulationsprogramm.dto

import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp

data class DokumentKlassifizierung(
    val typ: LieferantDokumentTyp?,
    val istGeschaeftsdokument: Boolean,
    val istDuplikat: Boolean,
    val referenzNummer: String?,
    val confidence: Double,
    val dokumentNummer: String?
) {
    fun typ(): LieferantDokumentTyp? = typ
    fun istGeschaeftsdokument(): Boolean = istGeschaeftsdokument
    fun istDuplikat(): Boolean = istDuplikat
    fun referenzNummer(): String? = referenzNummer
    fun confidence(): Double = confidence
    fun dokumentNummer(): String? = dokumentNummer

    fun sollIgnoriertWerden(): Boolean = typ == null

    companion object {
        @JvmStatic
        fun geschaeftsdokument(
            typ: LieferantDokumentTyp,
            dokumentNummer: String?,
            referenzNummer: String?,
            confidence: Double
        ): DokumentKlassifizierung =
            DokumentKlassifizierung(typ, true, false, referenzNummer, confidence, dokumentNummer)

        @JvmStatic
        fun sonstigesDokument(confidence: Double): DokumentKlassifizierung =
            DokumentKlassifizierung(LieferantDokumentTyp.SONSTIG, false, false, null, confidence, null)

        @JvmStatic
        fun nichtRelevant(): DokumentKlassifizierung =
            DokumentKlassifizierung(null, false, false, null, 0.0, null)
    }
}
