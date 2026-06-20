package org.example.kalkulationsprogramm.mapper

import org.example.kalkulationsprogramm.domain.Arbeitsgang
import org.example.kalkulationsprogramm.dto.Arbeitsgang.ArbeitsgangResponseDto
import org.example.kalkulationsprogramm.repository.ArbeitsgangStundensatzRepository
import org.springframework.stereotype.Component
import java.lang.reflect.Field

@Component
class ArbeitsgangMapper(private val stundensatzRepository: ArbeitsgangStundensatzRepository) {
    fun toArbeitsgangResponseDto(arbeitsgang: Arbeitsgang?): ArbeitsgangResponseDto? {
        if (arbeitsgang == null) return null
        val dto = ArbeitsgangResponseDto()
        val arbeitsgangId = arbeitsgang.readField("id") as? Long
        dto.id = arbeitsgangId
        dto.beschreibung = arbeitsgang.readField("beschreibung") as? String
        val abteilung = arbeitsgang.readField("abteilung")
        if (abteilung != null) {
            dto.abteilungId = abteilung.readField("id") as? Long
            dto.abteilungName = abteilung.readField("name") as? String
        }
        stundensatzRepository.findTopByArbeitsgangIdOrderByJahrDesc(arbeitsgangId).ifPresent { s ->
            dto.stundensatz = s.readField("satz") as? java.math.BigDecimal
            dto.stundensatzJahr = s.readField("jahr") as? Int
        }
        return dto
    }

    private fun Any.readField(name: String): Any? {
        val field: Field = findField(javaClass, name)
        field.isAccessible = true
        return field.get(this)
    }

    private fun findField(type: Class<*>, name: String): Field =
        try {
            type.getDeclaredField(name)
        } catch (_: NoSuchFieldException) {
            findField(type.superclass, name)
        }
}
