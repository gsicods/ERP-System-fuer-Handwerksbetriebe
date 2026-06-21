package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Gewerk
import org.example.kalkulationsprogramm.dto.GewerkDto
import org.example.kalkulationsprogramm.repository.GewerkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GewerkService(
    private val repository: GewerkRepository
) {
    @Transactional(readOnly = true)
    fun findAll(): List<GewerkDto> = repository.findAllByOrderByNameAsc().map(::toDto)

    @Transactional(readOnly = true)
    fun findAktiv(): List<GewerkDto> = repository.findByAktivTrueOrderByNameAsc().map(::toDto)

    @Transactional
    fun save(dto: GewerkDto): GewerkDto {
        val entity = dto.id?.let {
            repository.findById(it).orElseThrow { IllegalArgumentException("Gewerk nicht gefunden: ${dto.id}") }
        } ?: Gewerk()
        val name = dto.name?.trim()
        val bgName = dto.bgName?.trim()
        require(!name.isNullOrBlank()) { "Name darf nicht leer sein." }
        require(!bgName.isNullOrBlank()) { "BG-Name darf nicht leer sein." }
        require(dto.bgSatzProzent != null) { "BG-Satz (Prozent) ist Pflicht." }
        entity.name = name
        entity.bgName = bgName
        entity.bgSatzProzent = dto.bgSatzProzent
        entity.aktiv = dto.aktiv ?: true
        entity.bemerkung = dto.bemerkung
        return toDto(repository.save(entity))
    }

    @Transactional
    fun delete(id: Long) {
        repository.deleteById(id)
    }

    companion object {
        @JvmStatic
        fun toDto(e: Gewerk): GewerkDto {
            val dto = GewerkDto()
            dto.id = e.id
            dto.name = e.name
            dto.bgName = e.bgName
            dto.bgSatzProzent = e.bgSatzProzent
            dto.aktiv = e.aktiv
            dto.bemerkung = e.bemerkung
            return dto
        }
    }
}
