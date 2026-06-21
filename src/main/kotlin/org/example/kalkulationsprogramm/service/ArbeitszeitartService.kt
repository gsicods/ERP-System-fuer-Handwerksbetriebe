package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Arbeitszeitart
import org.example.kalkulationsprogramm.dto.Arbeitszeitart.ArbeitszeitartCreateDto
import org.example.kalkulationsprogramm.dto.Arbeitszeitart.ArbeitszeitartDto
import org.example.kalkulationsprogramm.repository.ArbeitszeitartRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArbeitszeitartService(
    private val repository: ArbeitszeitartRepository
) {
    fun findAllAktiv(): List<ArbeitszeitartDto> = repository.findAllAktiv().map(::toDto)

    fun findAll(): List<ArbeitszeitartDto> = repository.findAllSorted().map(::toDto)

    fun findById(id: Long): ArbeitszeitartDto =
        repository.findById(id).map(::toDto).orElseThrow { RuntimeException("Arbeitszeitart nicht gefunden: $id") }

    @Transactional
    fun create(dto: ArbeitszeitartCreateDto): ArbeitszeitartDto {
        val entity = Arbeitszeitart()
        entity.bezeichnung = dto.bezeichnung
        entity.beschreibung = dto.beschreibung
        entity.stundensatz = dto.stundensatz
        entity.aktiv = dto.isAktiv
        entity.sortierung = dto.sortierung
        return toDto(repository.save(entity))
    }

    @Transactional
    fun update(id: Long, dto: ArbeitszeitartCreateDto): ArbeitszeitartDto {
        val entity = repository.findById(id).orElseThrow { RuntimeException("Arbeitszeitart nicht gefunden: $id") }
        entity.bezeichnung = dto.bezeichnung
        entity.beschreibung = dto.beschreibung
        entity.stundensatz = dto.stundensatz
        entity.aktiv = dto.isAktiv
        entity.sortierung = dto.sortierung
        return toDto(repository.save(entity))
    }

    @Transactional
    fun delete(id: Long) {
        repository.deleteById(id)
    }

    @Transactional
    fun deaktivieren(id: Long) {
        val entity = repository.findById(id).orElseThrow { RuntimeException("Arbeitszeitart nicht gefunden: $id") }
        entity.aktiv = false
        repository.save(entity)
    }

    private fun toDto(entity: Arbeitszeitart): ArbeitszeitartDto =
        ArbeitszeitartDto(entity.id, entity.bezeichnung, entity.beschreibung, entity.stundensatz, entity.aktiv, entity.sortierung)
}
