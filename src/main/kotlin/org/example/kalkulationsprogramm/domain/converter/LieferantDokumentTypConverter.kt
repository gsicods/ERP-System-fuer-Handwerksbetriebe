package org.example.kalkulationsprogramm.domain.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp

@Converter(autoApply = true)
class LieferantDokumentTypConverter : AttributeConverter<LieferantDokumentTyp, String> {
    override fun convertToDatabaseColumn(attribute: LieferantDokumentTyp?): String? = attribute?.name

    override fun convertToEntityAttribute(dbData: String?): LieferantDokumentTyp? {
        val value = dbData?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        if (value.equals("EINGANGSRECHNUNG", ignoreCase = true)) return LieferantDokumentTyp.RECHNUNG
        return runCatching { LieferantDokumentTyp.valueOf(value) }.getOrNull()
    }
}
