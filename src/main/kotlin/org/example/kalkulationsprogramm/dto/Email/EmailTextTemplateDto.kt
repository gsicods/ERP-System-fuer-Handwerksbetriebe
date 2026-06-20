package org.example.kalkulationsprogramm.dto.Email

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import org.example.kalkulationsprogramm.domain.EmailTextTemplate
import org.example.kalkulationsprogramm.domain.EmailTextTemplateKategorie
import org.example.kalkulationsprogramm.service.EmailTextTemplateKategorien
import java.lang.reflect.Field
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EmailTextTemplateDto(
    var id: Long? = null,
    @field:NotBlank
    var dokumentTyp: String? = null,
    var kategorie: EmailTextTemplateKategorie? = null,
    @field:NotBlank
    var name: String? = null,
    @field:NotBlank
    var subjectTemplate: String? = null,
    @field:NotBlank
    var htmlBody: String? = null,
    var aktiv: Boolean? = null,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null,
) {
    fun applyToEntity(entity: EmailTextTemplate) {
        dokumentTyp?.let {
            entity.writeField("dokumentTyp", it.trim().uppercase())
        }
        entity.writeField("kategorie", kategorie ?: EmailTextTemplateKategorien.kategorieFuer(entity.readField("dokumentTyp") as? String))
        name?.let { entity.writeField("name", it.trim()) }
        subjectTemplate?.let { entity.writeField("subjectTemplate", it) }
        htmlBody?.let { entity.writeField("htmlBody", it) }
        aktiv?.let { entity.writeField("aktiv", it) }
    }

    companion object {
        @JvmStatic
        fun fromEntity(entity: EmailTextTemplate): EmailTextTemplateDto {
            val dokumentTyp = entity.readField("dokumentTyp") as? String
            val kategorie = entity.readField("kategorie") as? EmailTextTemplateKategorie
                ?: EmailTextTemplateKategorien.kategorieFuer(dokumentTyp)
            return EmailTextTemplateDto(
                id = entity.readField("id") as? Long,
                dokumentTyp = dokumentTyp,
                kategorie = kategorie,
                name = entity.readField("name") as? String,
                subjectTemplate = entity.readField("subjectTemplate") as? String,
                htmlBody = entity.readField("htmlBody") as? String,
                aktiv = entity.readField("aktiv") as? Boolean,
                createdAt = entity.readField("createdAt") as? OffsetDateTime,
                updatedAt = entity.readField("updatedAt") as? OffsetDateTime,
            )
        }
    }
}

private fun EmailTextTemplate.readField(name: String): Any? {
    val field: Field = EmailTextTemplate::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this)
}

private fun EmailTextTemplate.writeField(name: String, value: Any?) {
    val field: Field = EmailTextTemplate::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
}
