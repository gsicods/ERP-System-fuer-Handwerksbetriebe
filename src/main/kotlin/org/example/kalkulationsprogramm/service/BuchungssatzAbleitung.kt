package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Beleg
import org.example.kalkulationsprogramm.domain.BelegKategorie
import org.example.kalkulationsprogramm.domain.Sachkonto

object BuchungssatzAbleitung {
    const val KASSE = "Kasse"
    const val BANK = "Bank"
    const val PRIVATEINLAGE = "Privateinlage"
    const val PRIVATENTNAHME = "Privatentnahme"
    const val UNKLAR = "?"

    @JvmStatic
    fun ableiten(beleg: Beleg?): Buchungssatz {
        val kategorie = readField(beleg, "belegKategorie") as? BelegKategorie
            ?: return Buchungssatz(UNKLAR, UNKLAR)
        val sachkonto = readField(beleg, "sachkonto") as? Sachkonto
        val sachkontoLabel = sachkontoLabel(sachkonto)

        return when (kategorie) {
            BelegKategorie.KASSE_EINNAHME -> Buchungssatz(
                KASSE,
                if (sachkonto != null) sachkontoLabel else BANK,
            )
            BelegKategorie.KASSE_AUSGABE -> Buchungssatz(
                if (sachkonto != null) sachkontoLabel else UNKLAR,
                KASSE,
            )
            BelegKategorie.PRIVATEINLAGE -> Buchungssatz(
                KASSE,
                if (sachkonto != null) sachkontoLabel else PRIVATEINLAGE,
            )
            BelegKategorie.PRIVATENTNAHME -> Buchungssatz(
                if (sachkonto != null) sachkontoLabel else PRIVATENTNAHME,
                KASSE,
            )
            BelegKategorie.BANK,
            BelegKategorie.KREDITKARTE,
            BelegKategorie.SONSTIGER_BELEG,
            BelegKategorie.UNZUGEORDNET,
            -> Buchungssatz(UNKLAR, UNKLAR)
        }
    }

    private fun sachkontoLabel(sk: Sachkonto?): String {
        if (sk == null) return UNKLAR
        val nummer = readField(sk, "nummer") as? String
        val bezeichnung = readField(sk, "bezeichnung") as? String
        if (!nummer.isNullOrBlank() && !bezeichnung.isNullOrBlank()) {
            return "$nummer $bezeichnung"
        }
        if (!bezeichnung.isNullOrBlank()) return bezeichnung
        if (!nummer.isNullOrBlank()) return nummer
        return UNKLAR
    }

    private fun readField(target: Any?, fieldName: String): Any? {
        if (target == null) return null
        var type: Class<*>? = target.javaClass
        while (type != null) {
            try {
                val field = type.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(target)
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            }
        }
        return null
    }

    data class Buchungssatz(
        val soll: String,
        val haben: String,
    )
}
