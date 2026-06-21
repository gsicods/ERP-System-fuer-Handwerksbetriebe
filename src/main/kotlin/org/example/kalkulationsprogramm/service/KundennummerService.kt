package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.repository.KundeRepository
import org.example.kalkulationsprogramm.repository.KundenZaehlerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class KundennummerService(
    private val kundeRepository: KundeRepository,
    private val kundenZaehlerRepository: KundenZaehlerRepository
) {

    @Transactional(propagation = Propagation.REQUIRED)
    fun reserviereNaechsteKundennummer(): String {
        val zaehler = kundenZaehlerRepository.lockAndGet() ?: return generiereNaechsteKundennummer()
        val aktuelle = zaehler.naechsteNummer ?: FALLBACK_START
        zaehler.naechsteNummer = aktuelle + 1
        return aktuelle.toString()
    }

    fun generiereNaechsteKundennummer(): String =
        kundeRepository.findMaxKundennummer()
            .map { max ->
                try {
                    (max.toLong() + 1).toString()
                } catch (_: NumberFormatException) {
                    FALLBACK_START.toString()
                }
            }
            .orElse(FALLBACK_START.toString())

    companion object {
        private const val FALLBACK_START = 1000L
    }
}
