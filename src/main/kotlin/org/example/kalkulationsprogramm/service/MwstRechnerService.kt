package org.example.kalkulationsprogramm.service

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class MwstRechnerService {
    fun berechne(netto: BigDecimal?, brutto: BigDecimal?, satzProzent: BigDecimal?): MwstErgebnis {
        val gesetzt = (if (netto != null) 1 else 0) + (if (brutto != null) 1 else 0) + (if (satzProzent != null) 1 else 0)
        require(gesetzt >= 2) { "Mindestens zwei Werte (netto, brutto, satz) muessen angegeben sein" }
        validiereEingaben(netto, brutto, satzProzent)

        if (netto != null && satzProzent != null && brutto == null) {
            val mwst = netto.multiply(satzProzent).divide(HUNDERT, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
            val nettoR = scale2(netto)
            val bruttoR = scale2(nettoR.add(mwst))
            return MwstErgebnis(nettoR, bruttoR, satzProzent.setScale(SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP), mwst)
        }
        if (brutto != null && satzProzent != null && netto == null) {
            val faktor = BigDecimal.ONE.add(satzProzent.divide(HUNDERT, 10, RoundingMode.HALF_UP))
            val nettoR = brutto.divide(faktor, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
            val bruttoR = scale2(brutto)
            val mwst = scale2(bruttoR.subtract(nettoR))
            return MwstErgebnis(nettoR, bruttoR, satzProzent.setScale(SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP), mwst)
        }
        if (netto != null && brutto != null && satzProzent == null) {
            val nettoR = scale2(netto)
            val bruttoR = scale2(brutto)
            val mwst = bruttoR.subtract(nettoR)
            if (nettoR.signum() == 0) {
                if (bruttoR.signum() == 0) {
                    return MwstErgebnis(nettoR, bruttoR, BigDecimal.ZERO.setScale(SATZ_NACHKOMMASTELLEN), mwst)
                }
                throw IllegalArgumentException("Aus netto=0 mit brutto!=0 laesst sich kein Steuersatz ableiten")
            }
            val satz = mwst.multiply(HUNDERT).divide(nettoR, SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
            return MwstErgebnis(nettoR, bruttoR, satz, mwst)
        }
        val nettoR = scale2(netto!!)
        val bruttoR = scale2(brutto!!)
        val mwst = bruttoR.subtract(nettoR)
        val satzR = satzProzent?.setScale(SATZ_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
            ?: BigDecimal.ZERO.setScale(SATZ_NACHKOMMASTELLEN)
        return MwstErgebnis(nettoR, bruttoR, satzR, mwst)
    }

    fun nettoAusBrutto(brutto: BigDecimal?, satzProzent: BigDecimal?): BigDecimal? {
        if (brutto == null || satzProzent == null) return null
        val faktor = BigDecimal.ONE.add(satzProzent.divide(HUNDERT, 10, RoundingMode.HALF_UP))
        return brutto.divide(faktor, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
    }

    fun bruttoAusNetto(netto: BigDecimal?, satzProzent: BigDecimal?): BigDecimal? {
        if (netto == null || satzProzent == null) return null
        val mwst = netto.multiply(satzProzent).divide(HUNDERT, RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
        return scale2(netto).add(mwst)
    }

    private fun validiereEingaben(netto: BigDecimal?, brutto: BigDecimal?, satz: BigDecimal?) {
        if (netto != null) ausserhalb("netto", netto, MAX_BETRAG.negate(), MAX_BETRAG)
        if (brutto != null) ausserhalb("brutto", brutto, MAX_BETRAG.negate(), MAX_BETRAG)
        if (satz != null) ausserhalb("satzProzent", satz, MIN_SATZ, MAX_SATZ)
    }

    data class MwstErgebnis(
        val netto: BigDecimal,
        val brutto: BigDecimal,
        val satzProzent: BigDecimal,
        val mwstBetrag: BigDecimal
    )

    companion object {
        private const val RUNDUNGS_NACHKOMMASTELLEN = 2
        private const val SATZ_NACHKOMMASTELLEN = 2
        private val HUNDERT = BigDecimal("100")
        private val MAX_BETRAG = BigDecimal("1000000000")
        private val MIN_SATZ = BigDecimal.ZERO
        private val MAX_SATZ = BigDecimal("100")

        private fun ausserhalb(feld: String, v: BigDecimal, min: BigDecimal, max: BigDecimal) {
            if (v < min || v > max) throw IllegalArgumentException("$feld liegt ausserhalb des erlaubten Bereichs")
        }

        private fun scale2(v: BigDecimal): BigDecimal = v.setScale(RUNDUNGS_NACHKOMMASTELLEN, RoundingMode.HALF_UP)
    }
}
