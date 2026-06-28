package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Anrede
import org.example.kalkulationsprogramm.domain.SteuerberaterAnsprechpartner
import org.example.kalkulationsprogramm.domain.SteuerberaterKontakt
import org.example.kalkulationsprogramm.dto.SteuerberaterAnsprechpartnerDto
import org.example.kalkulationsprogramm.dto.SteuerberaterKontaktDto
import org.example.kalkulationsprogramm.repository.SteuerberaterKontaktRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SteuerberaterKontaktService(
    private val repository: SteuerberaterKontaktRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<SteuerberaterKontaktDto> =
        repository.findByAktivTrue().map(::toDto)

    @Transactional(readOnly = true)
    fun findById(id: Long): SteuerberaterKontaktDto? =
        repository.findById(id).map(::toDto).orElse(null)

    @Transactional
    fun speichern(dto: SteuerberaterKontaktDto): SteuerberaterKontaktDto {
        var steuerberater = if (dto.id != null) {
            repository.findById(dto.id!!)
                .orElseThrow { IllegalArgumentException("Steuerberater nicht gefunden: ${dto.id}") }
        } else {
            SteuerberaterKontakt()
        }

        steuerberater.name = dto.name
        steuerberater.email = dto.email
        steuerberater.telefon = dto.telefon
        steuerberater.ansprechpartner = dto.ansprechpartner
        steuerberater.autoProcessEmails = dto.autoProcessEmails ?: true
        steuerberater.aktiv = dto.aktiv ?: true
        steuerberater.notizen = dto.notizen
        steuerberater.gueltigAb = dto.gueltigAb
        steuerberater.gueltigBis = dto.gueltigBis
        if (dto.weitereEmails != null) {
            steuerberater.weitereEmails = dto.weitereEmails!!.toMutableSet()
        } else {
            steuerberater.weitereEmails.clear()
        }

        mergeAnsprechpartner(steuerberater, dto.ansprechpartnerListe)

        steuerberater = repository.save(steuerberater)
        return toDto(steuerberater)
    }

    private fun mergeAnsprechpartner(
        steuerberater: SteuerberaterKontakt,
        incoming: List<SteuerberaterAnsprechpartnerDto>?,
    ) {
        val bestehend = steuerberater.ansprechpartnerListe
        if (incoming.isNullOrEmpty()) {
            bestehend.clear()
            return
        }

        val byId = bestehend
            .filter { it.id != null }
            .associateBy { it.id }

        val lohnIndex = incoming.indexOfFirst { it.istLohnAnsprechpartner == true }
            .takeIf { it >= 0 } ?: 0

        val nachher = mutableListOf<SteuerberaterAnsprechpartner>()
        for ((index, input) in incoming.withIndex()) {
            val ansprechpartner = input.id?.let { byId[it] } ?: SteuerberaterAnsprechpartner().apply {
                this.steuerberater = steuerberater
            }
            ansprechpartner.anrede = Anrede.fromString(input.anrede)
            ansprechpartner.vorname = input.vorname
            ansprechpartner.nachname = input.nachname
            ansprechpartner.email = input.email
            ansprechpartner.telefon = input.telefon
            ansprechpartner.istLohnAnsprechpartner = index == lohnIndex
            ansprechpartner.notizen = input.notizen
            nachher.add(ansprechpartner)
        }

        bestehend.clear()
        bestehend.addAll(nachher)
    }

    @Transactional
    fun loeschen(id: Long) {
        repository.findById(id).ifPresent { steuerberater ->
            steuerberater.aktiv = false
            repository.save(steuerberater)
        }
    }

    @Transactional(readOnly = true)
    fun istSteuerberaterEmail(email: String): Boolean {
        if (repository.existsByEmailIgnoreCaseAndAktivTrue(email)) {
            return true
        }
        return repository.findByAktivTrue()
            .any { steuerberater ->
                steuerberater.weitereEmails.any { it.equals(email, ignoreCase = true) }
            }
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: String): SteuerberaterKontaktDto? {
        if (repository.existsByEmailIgnoreCaseAndAktivTrue(email)) {
            return repository.findByEmailIgnoreCase(email).map(::toDto).orElse(null)
        }
        return repository.findByAktivTrue()
            .firstOrNull { steuerberater ->
                steuerberater.weitereEmails.any { it.equals(email, ignoreCase = true) }
            }
            ?.let(::toDto)
    }

    private fun toDto(steuerberater: SteuerberaterKontakt): SteuerberaterKontaktDto =
        SteuerberaterKontaktDto(
            id = steuerberater.id,
            name = steuerberater.name,
            email = steuerberater.email,
            telefon = steuerberater.telefon,
            ansprechpartner = steuerberater.ansprechpartner,
            autoProcessEmails = steuerberater.autoProcessEmails,
            aktiv = steuerberater.aktiv,
            notizen = steuerberater.notizen,
            gueltigAb = steuerberater.gueltigAb,
            gueltigBis = steuerberater.gueltigBis,
            weitereEmails = ArrayList(steuerberater.weitereEmails),
            ansprechpartnerListe = steuerberater.ansprechpartnerListe.map(::toAnsprechpartnerDto),
        )

    private fun toAnsprechpartnerDto(ansprechpartner: SteuerberaterAnsprechpartner): SteuerberaterAnsprechpartnerDto =
        SteuerberaterAnsprechpartnerDto(
            id = ansprechpartner.id,
            anrede = ansprechpartner.anrede?.name,
            vorname = ansprechpartner.vorname,
            nachname = ansprechpartner.nachname,
            email = ansprechpartner.email,
            telefon = ansprechpartner.telefon,
            istLohnAnsprechpartner = ansprechpartner.istLohnAnsprechpartner,
            notizen = ansprechpartner.notizen,
        )
}
