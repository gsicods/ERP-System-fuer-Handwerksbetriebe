package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Krankenkasse
import org.example.kalkulationsprogramm.dto.KrankenkasseDto
import org.example.kalkulationsprogramm.repository.KrankenkasseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KrankenkasseService(
    private val repository: KrankenkasseRepository
) {
    @Transactional(readOnly = true)
    fun findAll(): List<KrankenkasseDto> = repository.findAllByOrderByNameAsc().map(::toDto)

    @Transactional(readOnly = true)
    fun findAktiv(): List<KrankenkasseDto> = repository.findByAktivTrueOrderByNameAsc().map(::toDto)

    @Transactional
    fun save(dto: KrankenkasseDto): KrankenkasseDto {
        val entity = dto.id?.let {
            repository.findById(it).orElseThrow { IllegalArgumentException("Krankenkasse nicht gefunden: ${dto.id}") }
        } ?: Krankenkasse()
        val name = dto.name?.trim()
        require(!name.isNullOrBlank()) { "Name darf nicht leer sein." }
        require(dto.zusatzbeitragProzent != null) { "Zusatzbeitrag (Prozent) ist Pflicht." }
        entity.name = name
        entity.kuerzel = dto.kuerzel
        entity.zusatzbeitragProzent = dto.zusatzbeitragProzent
        entity.aktiv = dto.aktiv ?: true
        entity.gueltigAb = dto.gueltigAb
        entity.bemerkung = dto.bemerkung
        return toDto(repository.save(entity))
    }

    @Transactional
    fun delete(id: Long) {
        repository.deleteById(id)
    }

    companion object {
        @JvmStatic
        fun toDto(e: Krankenkasse): KrankenkasseDto {
            val dto = KrankenkasseDto()
            dto.id = e.id
            dto.name = e.name
            dto.kuerzel = e.kuerzel
            dto.zusatzbeitragProzent = e.zusatzbeitragProzent
            dto.aktiv = e.aktiv
            dto.gueltigAb = e.gueltigAb
            dto.bemerkung = e.bemerkung
            return dto
        }
    }
}
