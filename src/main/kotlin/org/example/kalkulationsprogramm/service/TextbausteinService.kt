package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Dokumenttyp
import org.example.kalkulationsprogramm.domain.Textbaustein
import org.example.kalkulationsprogramm.domain.TextbausteinTyp
import org.example.kalkulationsprogramm.dto.Textbaustein.TextbausteinDto
import org.example.kalkulationsprogramm.repository.TextbausteinRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
class TextbausteinService(
    private val repository: TextbausteinRepository
) {
    @Transactional(readOnly = true)
    fun list(typ: String?): List<Textbaustein> {
        if (StringUtils.hasText(typ)) {
            return repository.findByTypOrderBySortOrderAscNameAsc(TextbausteinTyp.fromString(typ))
        }
        return repository.findAllWithDokumenttypen().sortedWith(
            compareBy<Textbaustein> { it.sortOrder ?: Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name ?: "" }
        )
    }

    fun create(dto: TextbausteinDto): Textbaustein {
        val entity = Textbaustein()
        dto.applyToEntity(entity)
        applyDokumenttypen(dto, entity)
        return repository.save(entity)
    }

    fun update(id: Long, dto: TextbausteinDto): Textbaustein {
        val entity = repository.findById(id).orElseThrow { IllegalArgumentException("Textbaustein nicht gefunden") }
        dto.applyToEntity(entity)
        applyDokumenttypen(dto, entity)
        return repository.save(entity)
    }

    fun delete(id: Long?) {
        if (id != null) repository.deleteById(id)
    }

    fun get(id: Long): Textbaustein =
        repository.findById(id).orElseThrow { IllegalArgumentException("Textbaustein nicht gefunden") }

    private fun applyDokumenttypen(dto: TextbausteinDto, entity: Textbaustein) {
        entity.dokumenttypen.clear()
        dto.dokumenttypen.asSequence()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .mapNotNull(Dokumenttyp::fromLabel)
            .forEach(entity.dokumenttypen::add)
    }
}
