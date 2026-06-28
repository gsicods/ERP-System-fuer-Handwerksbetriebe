package org.example.kalkulationsprogramm.service.miete

import org.example.kalkulationsprogramm.domain.miete.Raum
import org.example.kalkulationsprogramm.domain.miete.Verbrauchsgegenstand
import org.example.kalkulationsprogramm.domain.miete.Zaehlerstand
import org.example.kalkulationsprogramm.exception.NotFoundException
import org.example.kalkulationsprogramm.repository.miete.MietobjektRepository
import org.example.kalkulationsprogramm.repository.miete.RaumRepository
import org.example.kalkulationsprogramm.repository.miete.VerbrauchsgegenstandRepository
import org.example.kalkulationsprogramm.repository.miete.ZaehlerstandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RaumVerbrauchService(
    private val mietobjektRepository: MietobjektRepository,
    private val raumRepository: RaumRepository,
    private val verbrauchsgegenstandRepository: VerbrauchsgegenstandRepository,
    private val zaehlerstandRepository: ZaehlerstandRepository,
) {
    fun getRaeume(mietobjektId: Long): List<Raum> {
        val mietobjekt = mietobjektRepository.findById(mietobjektId)
            .orElseThrow { NotFoundException("Mietobjekt $mietobjektId nicht gefunden") }
        return raumRepository.findByMietobjektOrderByNameAsc(mietobjekt)
    }

    fun saveRaum(mietobjektId: Long, raum: Raum): Raum {
        val mietobjekt = mietobjektRepository.findById(mietobjektId)
            .orElseThrow { NotFoundException("Mietobjekt $mietobjektId nicht gefunden") }
        raum.mietobjekt = mietobjekt
        return raumRepository.save(raum)
    }

    fun deleteRaum(raumId: Long) {
        val raum = raumRepository.findById(raumId)
            .orElseThrow { NotFoundException("Raum $raumId nicht gefunden") }
        raumRepository.delete(raum)
    }

    fun getVerbrauchsgegenstaende(raumId: Long): List<Verbrauchsgegenstand> {
        val raum = raumRepository.findById(raumId)
            .orElseThrow { NotFoundException("Raum $raumId nicht gefunden") }
        return verbrauchsgegenstandRepository.findByRaumOrderByNameAsc(raum)
    }

    fun saveVerbrauchsgegenstand(
        raumId: Long,
        gegenstand: Verbrauchsgegenstand,
    ): Verbrauchsgegenstand {
        val raum = raumRepository.findById(raumId)
            .orElseThrow { NotFoundException("Raum $raumId nicht gefunden") }
        gegenstand.raum = raum
        return verbrauchsgegenstandRepository.save(gegenstand)
    }

    fun deleteVerbrauchsgegenstand(verbrauchsgegenstandId: Long) {
        val gegenstand = verbrauchsgegenstandRepository.findById(verbrauchsgegenstandId)
            .orElseThrow { NotFoundException("Verbrauchsgegenstand $verbrauchsgegenstandId nicht gefunden") }
        verbrauchsgegenstandRepository.delete(gegenstand)
    }

    fun saveZaehlerstand(
        verbrauchsgegenstandId: Long,
        eingabe: Zaehlerstand,
    ): Zaehlerstand {
        val gegenstand = verbrauchsgegenstandRepository.findById(verbrauchsgegenstandId)
            .orElseThrow { NotFoundException("Verbrauchsgegenstand $verbrauchsgegenstandId nicht gefunden") }

        val ziel = zaehlerstandRepository
            .findByVerbrauchsgegenstandAndAbrechnungsJahr(gegenstand, eingabe.abrechnungsJahr)
            .orElseGet(::Zaehlerstand)

        ziel.verbrauchsgegenstand = gegenstand
        ziel.abrechnungsJahr = eingabe.abrechnungsJahr
        ziel.stichtag = eingabe.stichtag
        ziel.stand = eingabe.stand
        ziel.kommentar = eingabe.kommentar

        berechneVerbrauch(ziel)

        val gespeichert = zaehlerstandRepository.save(ziel)
        aktualisiereNachfolgerVerbrauch(gegenstand, ziel.abrechnungsJahr)
        return gespeichert
    }

    fun getZaehlerstaende(verbrauchsgegenstandId: Long): List<Zaehlerstand> {
        val gegenstand = verbrauchsgegenstandRepository.findById(verbrauchsgegenstandId)
            .orElseThrow { NotFoundException("Verbrauchsgegenstand $verbrauchsgegenstandId nicht gefunden") }
        val werte = zaehlerstandRepository.findByVerbrauchsgegenstandOrderByAbrechnungsJahrDesc(gegenstand)
        return werte.sortedBy { it.abrechnungsJahr }
    }

    fun deleteZaehlerstand(zaehlerstandId: Long) {
        val zaehlerstand = zaehlerstandRepository.findById(zaehlerstandId)
            .orElseThrow { NotFoundException("Zaehlerstand $zaehlerstandId nicht gefunden") }
        val jahr = zaehlerstand.abrechnungsJahr
        val gegenstand = zaehlerstand.verbrauchsgegenstand
        zaehlerstandRepository.delete(zaehlerstand)
        aktualisiereNachfolgerVerbrauch(gegenstand, jahr?.minus(1))
    }

    private fun aktualisiereNachfolgerVerbrauch(
        gegenstand: Verbrauchsgegenstand?,
        jahr: Int?,
    ) {
        if (gegenstand == null || jahr == null) {
            return
        }
        val basis = if (jahr > 0) {
            zaehlerstandRepository
                .findByVerbrauchsgegenstandAndAbrechnungsJahr(gegenstand, jahr)
                .orElse(null)
        } else {
            null
        }
        val nachfolger = zaehlerstandRepository
            .findByVerbrauchsgegenstandAndAbrechnungsJahr(gegenstand, jahr + 1)
            .orElse(null)
        if (nachfolger != null) {
            nachfolger.verbrauch = if (basis?.stand != null && nachfolger.stand != null) {
                nachfolger.stand!!.subtract(basis.stand)
            } else {
                null
            }
            zaehlerstandRepository.save(nachfolger)
            aktualisiereNachfolgerVerbrauch(gegenstand, jahr + 1)
        }
    }

    private fun berechneVerbrauch(ziel: Zaehlerstand) {
        val gegenstand = ziel.verbrauchsgegenstand
        val jahr = ziel.abrechnungsJahr
        if (gegenstand == null || jahr == null || ziel.stand == null) {
            ziel.verbrauch = null
            return
        }
        zaehlerstandRepository
            .findByVerbrauchsgegenstandAndAbrechnungsJahr(gegenstand, jahr - 1)
            .ifPresentOrElse({ vorjahr ->
                ziel.verbrauch = if (vorjahr.stand != null) {
                    ziel.stand!!.subtract(vorjahr.stand)
                } else {
                    null
                }
            }, {
                ziel.verbrauch = null
            })
    }
}
