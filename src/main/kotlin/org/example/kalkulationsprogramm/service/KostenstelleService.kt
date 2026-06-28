package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Kostenstelle
import org.example.kalkulationsprogramm.domain.KostenstellenTyp
import org.example.kalkulationsprogramm.dto.KostenstelleDto
import org.example.kalkulationsprogramm.repository.KostenstelleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KostenstelleService(
    private val repository: KostenstelleRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<KostenstelleDto> =
        repository.findByAktivTrueOrderBySortierungAsc().map(::toDto)

    @Transactional(readOnly = true)
    fun findByTyp(typ: KostenstellenTyp): List<KostenstelleDto> =
        repository.findByTypAndAktivTrue(typ).map(::toDto)

    @Transactional(readOnly = true)
    fun findById(id: Long): KostenstelleDto? =
        repository.findById(id).map(::toDto).orElse(null)

    @Transactional
    fun speichern(dto: KostenstelleDto): KostenstelleDto {
        var kostenstelle = if (dto.id != null) {
            repository.findById(dto.id!!)
                .orElseThrow { IllegalArgumentException("Kostenstelle nicht gefunden: ${dto.id}") }
        } else {
            Kostenstelle()
        }

        kostenstelle.bezeichnung = dto.bezeichnung
        kostenstelle.typ = dto.typ
        kostenstelle.beschreibung = dto.beschreibung
        kostenstelle.istFixkosten = dto.isIstFixkosten
        kostenstelle.istInvestition = dto.isIstInvestition
        kostenstelle.aktiv = dto.isAktiv
        kostenstelle.sortierung = dto.sortierung ?: 0

        kostenstelle = repository.save(kostenstelle)
        return toDto(kostenstelle)
    }

    @Transactional
    fun loeschen(id: Long) {
        repository.findById(id).ifPresent { kostenstelle ->
            kostenstelle.aktiv = false
            repository.save(kostenstelle)
        }
    }

    @Transactional
    fun erstelleStandardKostenstellen() {
        if (repository.count() == 0L) {
            val lager = Kostenstelle().apply {
                bezeichnung = "Lager"
                typ = KostenstellenTyp.LAGER
                beschreibung = "Lagerbestand - Investitionen"
                istInvestition = true
                sortierung = 1
            }
            repository.save(lager)

            val gemeinkosten = Kostenstelle().apply {
                bezeichnung = "Gemeinkosten"
                typ = KostenstellenTyp.GEMEINKOSTEN
                beschreibung = "Fixkosten fuer Gemeinkostensatz"
                istFixkosten = true
                sortierung = 2
            }
            repository.save(gemeinkosten)
        }
    }

    private fun toDto(kostenstelle: Kostenstelle): KostenstelleDto =
        KostenstelleDto(
            id = kostenstelle.id,
            bezeichnung = kostenstelle.bezeichnung,
            typ = kostenstelle.typ,
            beschreibung = kostenstelle.beschreibung,
            isIstFixkosten = kostenstelle.istFixkosten,
            isIstInvestition = kostenstelle.istInvestition,
            isAktiv = kostenstelle.aktiv,
            sortierung = kostenstelle.sortierung,
        )
}
