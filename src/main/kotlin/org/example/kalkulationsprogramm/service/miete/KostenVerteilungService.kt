package org.example.kalkulationsprogramm.service.miete

import org.example.kalkulationsprogramm.domain.miete.Kostenposition
import org.example.kalkulationsprogramm.domain.miete.KostenpositionBerechnung
import org.example.kalkulationsprogramm.domain.miete.Kostenstelle
import org.example.kalkulationsprogramm.domain.miete.Verbrauchsgegenstand
import org.example.kalkulationsprogramm.domain.miete.Verteilungsschluessel
import org.example.kalkulationsprogramm.domain.miete.VerteilungsschluesselEintrag
import org.example.kalkulationsprogramm.exception.MietabrechnungValidationException
import org.example.kalkulationsprogramm.exception.NotFoundException
import org.example.kalkulationsprogramm.repository.miete.KostenpositionRepository
import org.example.kalkulationsprogramm.repository.miete.MieteKostenstelleRepository
import org.example.kalkulationsprogramm.repository.miete.MietobjektRepository
import org.example.kalkulationsprogramm.repository.miete.MietparteiRepository
import org.example.kalkulationsprogramm.repository.miete.VerbrauchsgegenstandRepository
import org.example.kalkulationsprogramm.repository.miete.VerteilungsschluesselEintragRepository
import org.example.kalkulationsprogramm.repository.miete.VerteilungsschluesselRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
@Transactional
class KostenVerteilungService(
    private val mietobjektRepository: MietobjektRepository,
    private val mietparteiRepository: MietparteiRepository,
    private val verbrauchsgegenstandRepository: VerbrauchsgegenstandRepository,
    private val kostenstelleRepository: MieteKostenstelleRepository,
    private val kostenpositionRepository: KostenpositionRepository,
    private val verteilungsschluesselRepository: VerteilungsschluesselRepository,
    private val verteilungsschluesselEintragRepository: VerteilungsschluesselEintragRepository,
) {
    fun getKostenstellen(mietobjektId: Long): List<Kostenstelle> =
        kostenstelleRepository.findByMietobjektIdOrderByNameAsc(mietobjektId)

    fun saveKostenstelle(mietobjektId: Long, kostenstelle: Kostenstelle): Kostenstelle {
        val mietobjekt = mietobjektRepository.findById(mietobjektId)
            .orElseThrow { NotFoundException("Mietobjekt $mietobjektId nicht gefunden") }
        val ziel = kostenstelle.id?.let { id ->
            kostenstelleRepository.findById(id)
                .orElseThrow { NotFoundException("Kostenstelle $id nicht gefunden") }
        } ?: Kostenstelle()

        ziel.mietobjekt = mietobjekt
        ziel.name = kostenstelle.name
        ziel.beschreibung = kostenstelle.beschreibung
        ziel.umlagefaehig = kostenstelle.umlagefaehig
        val standardSchluesselId = kostenstelle.standardSchluessel?.id
        ziel.standardSchluessel = if (standardSchluesselId != null) {
            verteilungsschluesselRepository.findById(standardSchluesselId)
                .orElseThrow { NotFoundException("Verteilungsschluessel $standardSchluesselId nicht gefunden") }
        } else {
            null
        }
        return kostenstelleRepository.save(ziel)
    }

    fun deleteKostenstelle(kostenstelleId: Long) {
        val kostenstelle = kostenstelleRepository.findById(kostenstelleId)
            .orElseThrow { NotFoundException("Kostenstelle $kostenstelleId nicht gefunden") }
        kostenstelleRepository.delete(kostenstelle)
    }

    fun getKostenpositionen(kostenstelleId: Long, jahr: Int?): List<Kostenposition> {
        val kostenstelle = kostenstelleRepository.findById(kostenstelleId)
            .orElseThrow { NotFoundException("Kostenstelle $kostenstelleId nicht gefunden") }
        return if (jahr != null) {
            kostenpositionRepository.findByKostenstelleAndAbrechnungsJahr(kostenstelle, jahr)
        } else {
            kostenpositionRepository.findByKostenstelle(kostenstelle)
        }
    }

    fun saveKostenposition(kostenstelleId: Long, kostenposition: Kostenposition): Kostenposition {
        val kostenstelle = kostenstelleRepository.findById(kostenstelleId)
            .orElseThrow { NotFoundException("Kostenstelle $kostenstelleId nicht gefunden") }
        val ziel = kostenposition.id?.let { id ->
            kostenpositionRepository.findById(id)
                .orElseThrow { NotFoundException("Kostenposition $id nicht gefunden") }
        } ?: Kostenposition()

        ziel.kostenstelle = kostenstelle
        ziel.abrechnungsJahr = kostenposition.abrechnungsJahr
        val berechnung = kostenposition.berechnung ?: KostenpositionBerechnung.BETRAG
        ziel.berechnung = berechnung

        var betrag: BigDecimal? = kostenposition.betrag
        if (berechnung == KostenpositionBerechnung.VERBRAUCHSFAKTOR) {
            betrag = betrag ?: BigDecimal.ZERO
        } else if (betrag == null) {
            throw MietabrechnungValidationException(
                "Das Feld 'Betrag' darf nicht leer sein.",
                "Kostenposition ohne Betrag kann nicht gespeichert werden.",
            )
        }
        ziel.betrag = betrag?.setScale(2, RoundingMode.HALF_UP)

        ziel.verbrauchsfaktor =
            if (berechnung == KostenpositionBerechnung.VERBRAUCHSFAKTOR && kostenposition.verbrauchsfaktor != null) {
                kostenposition.verbrauchsfaktor!!.setScale(5, RoundingMode.HALF_UP)
            } else {
                null
            }
        ziel.beschreibung = kostenposition.beschreibung
        ziel.belegNummer = kostenposition.belegNummer
        ziel.buchungsdatum = kostenposition.buchungsdatum

        val overrideId = kostenposition.verteilungsschluesselOverride?.id
        ziel.verteilungsschluesselOverride = if (overrideId != null) {
            verteilungsschluesselRepository.findById(overrideId)
                .orElseThrow { NotFoundException("Verteilungsschluessel $overrideId nicht gefunden") }
        } else {
            null
        }
        return kostenpositionRepository.save(ziel)
    }

    fun deleteKostenposition(kostenpositionId: Long) {
        val kostenposition = kostenpositionRepository.findById(kostenpositionId)
            .orElseThrow { NotFoundException("Kostenposition $kostenpositionId nicht gefunden") }
        kostenpositionRepository.delete(kostenposition)
    }

    fun copyKostenpositionenVonVorjahr(mietobjektId: Long, zielJahr: Int): Int {
        val vorjahr = zielJahr - 1
        val kostenstellen = kostenstelleRepository.findByMietobjektIdOrderByNameAsc(mietobjektId)
        var kopiert = 0
        for (kostenstelle in kostenstellen) {
            val vorjahrPositionen = kostenpositionRepository.findByKostenstelleAndAbrechnungsJahr(kostenstelle, vorjahr)
            val zielPositionen = kostenpositionRepository.findByKostenstelleAndAbrechnungsJahr(kostenstelle, zielJahr)
            if (zielPositionen.isNotEmpty()) {
                continue
            }
            for (quelle in vorjahrPositionen) {
                val kopie = Kostenposition()
                kopie.kostenstelle = kostenstelle
                kopie.abrechnungsJahr = zielJahr
                kopie.beschreibung = quelle.beschreibung
                kopie.berechnung = quelle.berechnung
                kopie.betrag = quelle.betrag
                kopie.verbrauchsfaktor = quelle.verbrauchsfaktor
                kopie.verteilungsschluesselOverride = quelle.verteilungsschluesselOverride
                kopie.buchungsdatum = LocalDate.now()
                kostenpositionRepository.save(kopie)
                kopiert++
            }
        }
        return kopiert
    }

    fun getVerteilungsschluessel(mietobjektId: Long): List<Verteilungsschluessel> {
        val mietobjekt = mietobjektRepository.findById(mietobjektId)
            .orElseThrow { NotFoundException("Mietobjekt $mietobjektId nicht gefunden") }
        return verteilungsschluesselRepository.findByMietobjektOrderByNameAsc(mietobjekt)
    }

    fun saveVerteilungsschluessel(
        mietobjektId: Long,
        schluessel: Verteilungsschluessel,
    ): Verteilungsschluessel {
        val mietobjekt = mietobjektRepository.findById(mietobjektId)
            .orElseThrow { NotFoundException("Mietobjekt $mietobjektId nicht gefunden") }
        val ziel = schluessel.id?.let { id ->
            verteilungsschluesselRepository.findById(id)
                .orElseThrow { NotFoundException("Verteilungsschluessel $id nicht gefunden") }
        } ?: Verteilungsschluessel()

        ziel.mietobjekt = mietobjekt
        ziel.name = schluessel.name
        ziel.beschreibung = schluessel.beschreibung
        ziel.typ = schluessel.typ

        val zielEintraege = ziel.eintraege ?: mutableListOf<VerteilungsschluesselEintrag>().also {
            ziel.eintraege = it
        }
        zielEintraege.clear()
        for (eingabe in schluessel.eintraege.orEmpty()) {
            val eintrag = eingabe.id?.let { id ->
                verteilungsschluesselEintragRepository.findById(id).orElse(VerteilungsschluesselEintrag())
            } ?: VerteilungsschluesselEintrag()

            val mietparteiId = eingabe.mietpartei?.id
            val mietpartei = mietparteiRepository.findById(requireNotNull(mietparteiId))
                .orElseThrow { NotFoundException("Mietpartei $mietparteiId nicht gefunden") }
            eintrag.verteilungsschluessel = ziel
            eintrag.mietpartei = mietpartei
            eintrag.anteil = eingabe.anteil
            eintrag.kommentar = eingabe.kommentar

            val verbrauchsgegenstandId = eingabe.verbrauchsgegenstand?.id
            eintrag.verbrauchsgegenstand = if (verbrauchsgegenstandId != null) {
                val gegenstand: Verbrauchsgegenstand = verbrauchsgegenstandRepository.findById(verbrauchsgegenstandId)
                    .orElseThrow { NotFoundException("Verbrauchsgegenstand $verbrauchsgegenstandId nicht gefunden") }
                gegenstand
            } else {
                null
            }
            zielEintraege.add(eintrag)
        }
        return verteilungsschluesselRepository.save(ziel)
    }

    fun deleteVerteilungsschluessel(id: Long) {
        val schluessel = verteilungsschluesselRepository.findById(id)
            .orElseThrow { NotFoundException("Verteilungsschluessel $id nicht gefunden") }
        verteilungsschluesselRepository.delete(schluessel)
    }
}
