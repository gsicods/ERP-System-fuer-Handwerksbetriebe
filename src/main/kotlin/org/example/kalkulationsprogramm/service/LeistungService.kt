package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Leistung
import org.example.kalkulationsprogramm.dto.Leistung.LeistungCreateDto
import org.example.kalkulationsprogramm.dto.Leistung.LeistungDto
import org.example.kalkulationsprogramm.mapper.LeistungMapper
import org.example.kalkulationsprogramm.repository.LeistungRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeistungService(
    private val leistungRepository: LeistungRepository,
    private val leistungMapper: LeistungMapper
) {

    @Transactional(readOnly = true)
    fun getAllLeistungen(): List<LeistungDto> =
        leistungRepository.findAll().mapNotNull(leistungMapper::toDto)

    @Transactional
    fun createLeistung(dto: LeistungCreateDto): LeistungDto {
        var leistung: Leistung = requireNotNull(leistungMapper.toEntity(dto))
        leistung = leistungRepository.save(leistung)
        return requireNotNull(leistungMapper.toDto(leistung))
    }

    @Transactional
    fun updateLeistung(id: Long, dto: LeistungCreateDto): LeistungDto {
        var leistung = leistungRepository.findById(id)
            .orElseThrow { RuntimeException("Leistung nicht gefunden: $id") }
        leistungMapper.updateEntity(leistung, dto)
        leistung = leistungRepository.save(leistung)
        return requireNotNull(leistungMapper.toDto(leistung))
    }

    @Transactional
    fun deleteLeistung(id: Long) {
        leistungRepository.deleteById(id)
    }
}
