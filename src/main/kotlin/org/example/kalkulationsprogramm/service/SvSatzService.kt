package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.SvSatz
import org.example.kalkulationsprogramm.domain.SvSatzTyp
import org.example.kalkulationsprogramm.dto.SvSatzDto
import org.example.kalkulationsprogramm.repository.SvSatzRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SvSatzService(
    private val repository: SvSatzRepository
) {
    @Transactional(readOnly = true)
    fun findAll(): List<SvSatzDto> = repository.findAllByOrderBySatzTypAscGueltigAbDesc().map(::toDto)

    @Transactional
    fun save(dto: SvSatzDto): SvSatzDto {
        val entity = dto.id?.let {
            repository.findById(it).orElseThrow { IllegalArgumentException("SV-Satz nicht gefunden: ${dto.id}") }
        } ?: SvSatz()
        val satzTyp = dto.satzTyp
        require(!satzTyp.isNullOrBlank()) { "Satz-Typ ist Pflicht." }
        require(dto.prozent != null) { "Prozent ist Pflicht." }
        require(dto.gueltigAb != null) { "Gueltig-ab-Datum ist Pflicht." }
        val typ = try {
            SvSatzTyp.valueOf(satzTyp)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Unbekannter SV-Satz-Typ: $satzTyp")
        }
        entity.satzTyp = typ
        entity.prozent = dto.prozent
        entity.gueltigAb = dto.gueltigAb
        entity.beschreibung = dto.beschreibung
        return try {
            toDto(repository.saveAndFlush(entity))
        } catch (_: DataIntegrityViolationException) {
            throw IllegalArgumentException(
                "Fuer den Typ $typ existiert bereits ein Satz mit dem Datum ${dto.gueltigAb}. Bitte den bestehenden Eintrag bearbeiten oder ein anderes Gueltig-ab-Datum waehlen."
            )
        }
    }

    @Transactional
    fun delete(id: Long) {
        repository.deleteById(id)
    }

    companion object {
        @JvmStatic
        fun toDto(e: SvSatz): SvSatzDto {
            val dto = SvSatzDto()
            dto.id = e.id
            dto.satzTyp = e.satzTyp?.name
            dto.prozent = e.prozent
            dto.gueltigAb = e.gueltigAb
            dto.beschreibung = e.beschreibung
            return dto
        }
    }
}
